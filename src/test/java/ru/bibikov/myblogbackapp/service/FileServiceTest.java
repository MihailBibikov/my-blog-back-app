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
import ru.bibikov.myblogbackapp.model.Post;
import ru.bibikov.myblogbackapp.repository.FileRepository;
import ru.bibikov.myblogbackapp.utils.DataUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
        assertTrue(Files.exists(savedFilePath),"Файл должен быть сохранен на диске");

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
        Post post= DataUtil.getPostPersisted();
        String example="a2527c38-6aae-49ab-be9d-93ef621b582f.png";

        when(repository.findById(anyLong())).thenReturn(Optional.ofNullable(post));

        String image=service.getImage(postId);

        assertNotNull(image);
        assertEquals(example,image);

        verify(repository).findById(postId);
    }

    @Test
    void updatePostService() {
        Post post=DataUtil.getPostPersisted();
        String example="a2527c38-6aae-49ab-be9d-93ef621b582f.png";

        when(repository.findById(anyLong())).thenReturn(Optional.ofNullable(post));
        when(repository.save(any(Post.class))).thenReturn(post);

        service.updatePostService(post.getId(),example);

        verify(repository).findById(post.getId());
        verify(repository).save(any(Post.class));
    }
}
