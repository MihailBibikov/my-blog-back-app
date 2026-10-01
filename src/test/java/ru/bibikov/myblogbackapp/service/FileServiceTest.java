package ru.bibikov.myblogbackapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import ru.bibikov.myblogbackapp.exception.file.FileStorageException;
import ru.bibikov.myblogbackapp.repository.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository repository;

    @InjectMocks
    private FileService service;

    @TempDir
    Path tempDir;

    @Test
    void saveImage() throws Exception{
        ReflectionTestUtils.setField(service,"uploadDir",tempDir.toString());

        byte[] fileContent="fake image data".getBytes();

        MockMultipartFile multipartFile=new MockMultipartFile(
                "image",
                "my_photo.JPG",
                "image/jpeg",
                fileContent
        );

        String savedFileName=service.saveImage(multipartFile);

        assertNotNull(savedFileName);
        assertTrue(savedFileName.endsWith(".jpg"));
        assertTrue(savedFileName.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$"));
        Path savedFilePath=tempDir.resolve(savedFileName);
        assertTrue(Files.exists(savedFilePath),"Файд должен быть созранен на диске");

        byte[] savedContent=Files.readAllBytes(savedFilePath);
        assertArrayEquals(fileContent,savedContent);
    }

    @Test
    void saveImageFileStorageException() throws IOException {
        ReflectionTestUtils.setField(service,"uploadDir",tempDir.toString());

        MockMultipartFile faultyFile=mock(MockMultipartFile.class);
        when(faultyFile.getOriginalFilename()).thenReturn("bad_photo.png");
        when(faultyFile.getInputStream()).thenThrow(new IOException("Disk is full"));

        FileStorageException exception=assertThrows(
                FileStorageException.class,
                ()->service.saveImage(faultyFile)
        );

        assertTrue(exception.getMessage().contains("Failed to save image"));

    }

    @Test
    void getImageSuccess() {
        Long postId=1L;
        String example="a2527c38-6aae-49ab-be9d-93ef621b582f.png";

        when(repository.getImage(postId)).thenReturn(example);

        String image=service.getImage(postId);

        assertNotNull(image);
        assertEquals(example,image);

        verify(repository).getImage(postId);
    }

    @Test
    void updatePostService() {
        Long postId=1L;
        String example="a2527c38-6aae-49ab-be9d-93ef621b582f.png";

        doNothing().when(repository).updateImage(postId,example);

        service.updatePostService(postId,example);

        verify(repository).updateImage(postId,example);
    }
}