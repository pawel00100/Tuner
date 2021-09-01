package com.tuner.recorded_files;

import com.tuner.model.server_requests.RecordedFile;
import com.tuner.persistence.db.RecordedFileDAO;
import com.tuner.settings.SettingsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecordListProvider {

    @Autowired
    RecordedFileDAO dao;

    @Value("${recording.location}")
    String location;

    public RecordListProvider(@Autowired SettingsProvider settingsProvider) {
        settingsProvider.subscribe("tvheadened.url", c -> location = c);

    }

    public void registerRecording(RecordedFile file) {
        dao.add(file);
    }


    public List<RecordedFile> getRecordings() {
        var files = Arrays.stream(new File(location).listFiles())
                .filter(f -> f.getPath().endsWith(".mp4"))
                .map(File::getName)
                .collect(Collectors.toSet());

        return dao.getAll().stream()
                .filter(e -> files.contains(e.getFilename()))
                .collect(Collectors.toList());
    }

}
