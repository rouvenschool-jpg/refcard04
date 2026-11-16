package ch.bbw.architecturerefcard04;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageService {

    private final String UPLOAD_DIR = "./uploads/";

    public void storeFile(MultipartFile file) {

        // normalize the file path
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        // save the file on the local file system
        try {
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFiles() {
        File folder = new File(UPLOAD_DIR);
        return Arrays.stream(folder.listFiles())
                .map(file -> file.getName())
                .collect(Collectors.toList());
    }
}
