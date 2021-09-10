package com.tuner.recording_manager;

import com.tuner.connector_to_tvh.ChannelProvider;
import com.tuner.recorded_files.RecordListProvider;
import com.tuner.recorder.MockRecorder;
import com.tuner.recorder.Recorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.quartz.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;


class RecorderManagerTest {

    Recorder recorder = spy(new MockRecorder());
    RecordListProvider recordListProvider = mock(RecordListProvider.class);
    Scheduler scheduler = mock(Scheduler.class);
    ChannelProvider channelProvider = mock(ChannelProvider.class);

    private RecorderManager recorderManagerSpied;

    @BeforeEach
    void beforeAll() {
        RecorderManager recorderManager = new RecorderManager(recorder, recordListProvider, scheduler, channelProvider);
        recorderManagerSpied = spy(recorderManager);
    }

    @Test
    void scheduleInFuture() throws SchedulerException {
        RecordingOrderInternal order = getOrder("channel1", "program1", 1, 2);

        recorderManagerSpied.scheduleRecording(order);

        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void scheduleNow() throws SchedulerException {
        RecordingOrderInternal order = getOrder("channel1", "program1", -1, 2);

        recorderManagerSpied.scheduleRecording(order);

        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void scheduleMultiple() throws SchedulerException {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", 3, 4);
        RecordingOrderInternal order2 = getOrder("channel1", "program1", -1, 2);
        RecordingOrderInternal order3 = getOrder("channel1", "program1", 5, 6);

        recorderManagerSpied.scheduleRecording(order1);
        recorderManagerSpied.scheduleRecording(order2);
        recorderManagerSpied.scheduleRecording(order3);

        verify(scheduler, times(3)).scheduleJob(argThat(startRecordingMatcher()), any());
    }

    @Test
    void scheduleMultipleOverlapping() throws SchedulerException {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", 2, 4);
        RecordingOrderInternal order2 = getOrder("channel1", "program1", 3, 5);
        RecordingOrderInternal order3 = getOrder("channel1", "program1", -1, 3);

        recorderManagerSpied.scheduleRecording(order1);
        recorderManagerSpied.scheduleRecording(order2);
        recorderManagerSpied.scheduleRecording(order3);

        verify(scheduler, times(1)).scheduleJob(argThat(startRecordingMatcher()), any());
    }

    @Test
    void removeScheduledInFuture() throws SchedulerException {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", 3, 4);
        RecordingOrderInternal order2 = getOrder("channel1", "program1", 7, 8);
        RecordingOrderInternal order3 = getOrder("channel1", "program1", 5, 6);

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));

        verify(scheduler, times(3)).scheduleJob(argThat(startRecordingMatcher()), any());

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order3));

        verify(scheduler, times(3)).scheduleJob(argThat(startRecordingMatcher()), any());
        verify(scheduler, times(1)).unscheduleJob(any());
        verify(scheduler, times(1)).deleteJob(any());
    }

    @Test
    void addWhileRecording() throws Exception {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", -1, 2);
        RecordingOrderInternal order2 = getOrder("channel1", "program1", 3, 5); //ok
        RecordingOrderInternal order3 = getOrder("channel1", "program1", -1, 3); //collides

        List<JobDetail> jobs = new ArrayList<>();

        doAnswer(invocationOnMock -> addToJobs(jobs, invocationOnMock)).when(scheduler).scheduleJob(any(), any());

        recorderManagerSpied.scheduleRecording(order1);

        executeJob(startRecording(jobs));

        recorderManagerSpied.scheduleRecording(order2);
        recorderManagerSpied.scheduleRecording(order3);

        executeJob(stopRecording(jobs));

        verify(recorder, times(1)).start(any(), any());
        verify(recorder, times(1)).stop();
        verify(scheduler, times(2)).scheduleJob(argThat(startRecordingMatcher()), any());
    }

    @Test
    void removeCurrentlyRecorded() throws Exception {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", -1, 4);
        RecordingOrderInternal order2 = getOrder("channel1", "program1", 7, 8);
        RecordingOrderInternal order3 = getOrder("channel1", "program1", 5, 6);

        List<JobDetail> jobs = new ArrayList<>();

        doAnswer(invocationOnMock -> addToJobs(jobs, invocationOnMock)).when(scheduler).scheduleJob(any(), any());

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));

        executeJob(startRecording(jobs));

        verify(recorder, times(1)).start(any(), any());

        recorderManagerSpied.scheduleRecording(Arrays.asList(order2, order3));

        verify(recorder, times(1)).stop();
    }

    private static Object addToJobs(List<JobDetail> jobs, InvocationOnMock invocationOnMock) {
        jobs.add(invocationOnMock.getArgument(0));
        return null;
    }

    private static Class<? extends Job> stopRecording(List<JobDetail> jobs) {
        return jobs.stream().filter(jd -> jd.getJobClass().equals(RecorderManager.StopRecordingJob.class)).findFirst().get().getJobClass();
    }

    private static Class<? extends Job> startRecording(List<JobDetail> jobs) {
        return jobs.stream().filter(jd -> jd.getJobClass().equals(RecorderManager.StartRecordingJob.class)).findFirst().get().getJobClass();
    }

    private static ArgumentMatcher<JobDetail> startRecordingMatcher() {
        return jd -> jd.getJobClass().equals(RecorderManager.StartRecordingJob.class);
    }

    private void executeJob(Class<?> innerClass) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, JobExecutionException {
        Constructor<?> ctor = innerClass.getDeclaredConstructor(RecorderManager.class);

        Job innerInstance = (Job) ctor.newInstance(recorderManagerSpied);
        innerInstance.execute(null);
    }

    private static RecordingOrderInternal getOrder(String channelId, String programName, int startOffset, int endOffset) {
        return new RecordingOrderInternal(
                channelId,
                programName,
                ZonedDateTime.now().plus(startOffset, ChronoUnit.MINUTES),
                ZonedDateTime.now().plus(endOffset, ChronoUnit.MINUTES),
                true);
    }
}