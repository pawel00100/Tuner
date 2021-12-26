package com.tuner.recorder;

import com.tuner.model.server_requests.RecordedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class Converter {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @Value("${converter.enabled}")
    boolean enabled;
    @Value("${recording.location}")
    String originalLocation;
    @Value("${converter.converted.location}")
    String convertedLocation;
    @Value("${converter.ffmpeg.exec}")
    String ffmpegLocation;
    @Value("${converter.codec.video}")
    String videoCodec;
    @Value("${converter.bitrate.sd}")
    int SDBitrate;
    @Value("${converter.bitrate.hd}")
    int HDBitrate;

    public void convert(RecordedFile recordedFile) {
        if (enabled) {
            executor.submit(() -> task(recordedFile));
        }
    }

    public void task(RecordedFile recordedFile) {
        int bitrate = (recordedFile.getChannel().isHD()) ? HDBitrate : SDBitrate;
        try {
            String command = ffmpegLocation + " " +
                    "-nostats -loglevel 0 " +
                    "-i \"" + originalLocation + "\\" + recordedFile.getFilename() + "\" " +
                    "-vf yadif " +
                    "-codec:v " + videoCodec + " " +
                    "-b:v " + bitrate + "k " +
                    "\"" + convertedLocation + "\\" + recordedFile.getFilename() + "\"";
            log.info("Converting program " + recordedFile.getProgramName() + " with command " + command);

            var runtime = Runtime.getRuntime();
            var process = runtime.exec(command);

            process.waitFor();
            log.info("Converted program " + recordedFile.getProgramName());
        } catch (Exception ex) {
            log.error("Failed converting record");
        }
    }
}
