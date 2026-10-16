package ru.bibikov.myblogbackapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import ru.bibikov.myblogbackapp.exception.global.GlobalExceptionHandler;
import ru.bibikov.myblogbackapp.exception.post.PostWithIdNotFound;
import ru.bibikov.myblogbackapp.service.FileService;

import java.nio.file.Files;
import java.nio.file.Path;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageRestControllerTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileService service;

    @InjectMocks
    private ImageRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(){
        mockMvc= MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addOrUpdateImage() throws Exception {

        Long postId=111L;
        String expectedImagePath="/uploads/images/test-post.jpg";

        MockMultipartFile mockFile=new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        when(service.saveImage(any(MultipartFile.class))).thenReturn(expectedImagePath);

        mockMvc.perform(multipart("/api/posts/{id}/image",postId)
                .file(mockFile)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());

        verify(service).saveImage(any(MultipartFile.class));
        verify(service).updatePostService(eq(postId),eq(expectedImagePath));
    }

    @Test
    void addOrUpdateImageWithEmptyFile()throws Exception{
        Long postId=111L;
        MockMultipartFile emptyFile=new MockMultipartFile(
                "image",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        when(service.saveImage(any(MultipartFile.class))).thenThrow(new IllegalArgumentException("Не правильно набран контент"));

        mockMvc.perform(multipart("/api/posts/{id}/image",postId)
                .file(emptyFile)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isBadRequest());

        verify(service).saveImage(any(MultipartFile.class));
        verify(service,never()).updatePostService(anyLong(),anyString());
    }

    @Test
    void getImage() throws Exception {
        Long postId=111L;
        String fileName="test-Image.png";

        Path imageFile=tempDir.resolve(fileName);
        Files.write(imageFile,"fake image content".getBytes());

        when(service.getImage(postId)).thenReturn(fileName);
        when(service.getUploadDir()).thenReturn(tempDir.toString());

        mockMvc.perform(get("/api/posts/{id}/image",postId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(Files.readAllBytes(imageFile)));

        verify(service).getImage(postId);
        verify(service).getUploadDir();
    }

    @Test
    void getImagePostNotFound() throws Exception {
        Long postId=999L;
        when(service.getImage(postId)).thenThrow(new PostWithIdNotFound("Пост с таким ID не найден "));

        mockMvc.perform(get("/api/posts/{id}/image",postId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Пост с таким ID не найден "));

        verify(service).getImage(postId);
        verify(service,never()).getUploadDir();
    }

    @Test
    void getImageFileNotFoundOnDisk() throws Exception {
        Long postId=999L;
        String fileName="non-existent-file.png";

        Path emptyTempDir=Files.createTempDirectory("empty");

        when(service.getImage(postId)).thenReturn(fileName);
        when(service.getUploadDir()).thenReturn(emptyTempDir.toString());

        mockMvc.perform(get("/api/posts/{id}/image",postId))
                .andExpect(status().isInternalServerError());
        verify(service).getImage(postId);
        verify(service).getUploadDir();
    }



}
