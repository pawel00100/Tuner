package com.tuner.recording_manager;

import com.tuner.connector_to_tvh.ChannelProvider;
import com.tuner.model.server_requests.Channel;
import com.tuner.recorded_files.RecordListProvider;
import com.tuner.recorder.Converter;
import com.tuner.recorder.MockRecorder;
import com.tuner.recorder.Recorder;
import com.tuner.settings.SettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.quartz.*;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;


class RecorderManagerTest {

    Recorder recorder = spy(new MockRecorder());
    Recorder recorder2 = spy(new MockRecorder());
    Recorder recorder3 = spy(new MockRecorder());
    List<Recorder> recorders = Arrays.asList(recorder, recorder2, recorder3);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    RecordListProvider recordListProvider = mock(RecordListProvider.class);
    Scheduler scheduler = mock(Scheduler.class);
    ChannelProvider channelProvider = spy(new ChannelProvider(mock(SettingsProvider.class)));
    Converter converter = mock(Converter.class);

    private RecorderManager recorderManagerSpied;

    @BeforeEach
    void beforeAll() {
        RecorderManager recorderManager = new RecorderManager(applicationContext, recordListProvider, scheduler, converter);
        recorderManagerSpied = spy(recorderManager);

        List<Channel> channels = Arrays.asList(
                new Channel("channel1", "Channel 1", "mux1", "Mux 1"),
                new Channel("channel2", "Channel 2", "mux1", "Mux 1"),
                new Channel("channel3", "Channel 3", "mux1", "Mux 1"),
                new Channel("channel4", "Channel 4", "mux2", "Mux 2"),
                new Channel("channel5", "Channel 5", "mux3", "Mux 3"));
        doAnswer(i -> channels).when(channelProvider).getChannelList();
        var num = new AtomicInteger(0);
        doAnswer(i -> recorders.get(num.getAndIncrement())).when(applicationContext).getBean(Recorder.class);
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

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));

        verify(scheduler, times(3)).scheduleJob(argThat(startRecordingMatcher()), any());
    }

    @Test
    void scheduleOverlappingSameMux() throws SchedulerException {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", 3, 6);
        RecordingOrderInternal order2 = getOrder("channel1", "program1", 2, 7);
        RecordingOrderInternal order3 = getOrder("channel1", "program1", 4, 5);

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));

        verify(scheduler, times(3)).scheduleJob(argThat(startRecordingMatcher()), any());
    }

    @Test
    void scheduleOverlappingDifferentMuxes() throws SchedulerException {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", 2, 4);
        RecordingOrderInternal order2 = getOrder("channel4", "program1", 3, 5);
        RecordingOrderInternal order3 = getOrder("channel5", "program1", -1, 3);

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));

        verify(scheduler, times(1)).scheduleJob(argThat(startRecordingMatcher()), any());
    }

    @Test
    void removeScheduledInFuture() throws SchedulerException {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", 3, 4);
        RecordingOrderInternal order2 = getOrder("channel1", "program1", 7, 8);
        RecordingOrderInternal order3 = getOrder("channel1", "program1", 5, 6);
        RecordingOrderInternal order4 = getOrder("channel4", "program1", 9, 10);

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3, order4));

        verify(scheduler, times(4)).scheduleJob(argThat(startRecordingMatcher()), any());

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order3));

        verify(scheduler, times(4)).scheduleJob(argThat(startRecordingMatcher()), any());
        verify(scheduler, times(2)).unscheduleJob(any());
        verify(scheduler, times(2)).deleteJob(any());
    }

    @Test
    void addWhileRecording() throws Exception {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", -1, 2);
        RecordingOrderInternal order2 = getOrder("channel4", "program1", 3, 5); //ok
        RecordingOrderInternal order3 = getOrder("channel4", "program1", -1, 3); //collides

        List<JobDetail> jobs = new ArrayList<>();

        doAnswer(invocationOnMock -> addToJobs(jobs, invocationOnMock)).when(scheduler).scheduleJob(any(), any());

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));

        executeJob(startRecordingJobDetail(jobs));


        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));


        executeJob(stopRecordingJobDetail(jobs));

        verify(recorder, times(1)).start(any(), any(), any());
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

        executeJob(startRecordingJobDetail(jobs));

        verify(recorder, times(1)).start(any(), any(), any());

        recorderManagerSpied.scheduleRecording(Arrays.asList(order2, order3));

        verify(recorder, times(1)).stop();
    }

    @Test
    void recordMultiple() throws Exception {
        RecordingOrderInternal order1 = getOrder("channel1", "program1", -1, 4);
        RecordingOrderInternal order2 = getOrder("channel2", "program1", 0, 5);
        RecordingOrderInternal order3 = getOrder("channel3", "program1", 1, 2);

        List<JobDetail> jobs = new ArrayList<>();

        doAnswer(invocationOnMock -> addToJobs(jobs, invocationOnMock)).when(scheduler).scheduleJob(any(), any());

        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2));
        executeJob(startRecordingJobDetail(jobs, 0));
        executeJob(startRecordingJobDetail(jobs, 1));
        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));
        executeJob(startRecordingJobDetail(jobs, 2));
        recorderManagerSpied.scheduleRecording(Arrays.asList(order1, order2, order3));


        executeJob(stopRecordingJobDetail(jobs, 0));
        executeJob(stopRecordingJobDetail(jobs, 1));
        executeJob(stopRecordingJobDetail(jobs, 2));

        verify(recorder, times(1)).start(any(), any(), any());
        verify(recorder2, times(1)).start(any(), any(), any());
        verify(recorder3, times(1)).start(any(), any(), any());
        verify(applicationContext, times(3)).getBean(Recorder.class);
        verify(recorder, times(1)).stop();
        verify(recorder2, times(1)).stop();
        verify(recorder3, times(1)).stop();
        verify(scheduler, times(3)).scheduleJob(argThat(startRecordingMatcher()), any());
    }

    private static Object addToJobs(List<JobDetail> jobs, InvocationOnMock invocationOnMock) {
        jobs.add(invocationOnMock.getArgument(0));
        return null;
    }

    private static JobDetail stopRecordingJobDetail(List<JobDetail> jobs) {
        return stopRecordingJobDetail(jobs, 0);
    }

    private static JobDetail stopRecordingJobDetail(List<JobDetail> jobs, int num) {
        return jobs.stream().filter(jd -> jd.getJobClass().equals(RecorderManager.StopRecordingJob.class)).toList().get(num);
    }

    private static JobDetail startRecordingJobDetail(List<JobDetail> jobs) {
        return startRecordingJobDetail(jobs, 0);
    }

    private static JobDetail startRecordingJobDetail(List<JobDetail> jobs, int num) {
        return jobs.stream().filter(jd -> jd.getJobClass().equals(RecorderManager.StartRecordingJob.class)).toList().get(num);
    }

    private static ArgumentMatcher<JobDetail> startRecordingMatcher() {
        return jd -> jd.getJobClass().equals(RecorderManager.StartRecordingJob.class);
    }

    private void executeJob(JobDetail jobDetail) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, JobExecutionException {
        Class<? extends Job> innerClass = jobDetail.getJobClass();

        Constructor<?> ctor = innerClass.getDeclaredConstructor(RecorderManager.class);
        Job innerInstance = (Job) ctor.newInstance(recorderManagerSpied);
        JobExecutionContext context = mock(JobExecutionContext.class);

        doAnswer(i -> jobDetail).when(context).getJobDetail();

        innerInstance.execute(context);
    }

    private RecordingOrderInternal getOrder(String channelId, String programName, int startOffset, int endOffset) {
        return new RecordingOrderInternal(
                channelProvider.getChannel(channelId),
                programName,
                ZonedDateTime.now().plus(startOffset, ChronoUnit.MINUTES),
                ZonedDateTime.now().plus(endOffset, ChronoUnit.MINUTES),
                true);
    }
}