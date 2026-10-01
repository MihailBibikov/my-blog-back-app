package ru.bibikov.myblogbackapp.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bibikov.myblogbackapp.dto.PostPreviewDto;
import ru.bibikov.myblogbackapp.dto.PostResponse;
import ru.bibikov.myblogbackapp.dto.UpdatePostRequest;
import ru.bibikov.myblogbackapp.exception.global.GlobalExceptionHandler;

import ru.bibikov.myblogbackapp.exception.post.PostWithIdNotFound;
import ru.bibikov.myblogbackapp.model.Post;
import ru.bibikov.myblogbackapp.service.PostService;
import ru.bibikov.myblogbackapp.utils.DataUtil;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PostRestControllerTest {
    @Mock
    private PostService service;

    @InjectMocks
    private PostRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(){
        mockMvc= MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createPost() throws Exception{
        String requestJson= """
                {
                    "title": "New post",
                    "text": "Text posts",
                    "tags": ["spring","test"]
                }""";

        PostPreviewDto createdDto=DataUtil.getPostPreviewDto();

        when(service.createPost(anyString(),anyString(),anyList())).thenReturn(createdDto);

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdDto.getId()))
                .andExpect(jsonPath("$.title").value(createdDto.getTitle()))
                .andExpect(jsonPath("$.text").value(createdDto.getText()))
                .andExpect(jsonPath("$.tags[0]").value(createdDto.getTags().get(0)))
                .andExpect(jsonPath("$.tags[1]").value(createdDto.getTags().get(1)));

        verify(service).createPost(anyString(),anyString(),anyList());
    }

    @Test
    void getPostsSuccess() throws Exception {
        String search="Post";
        int pageNumber=1;
        int pageSize=5;

        PostResponse example=PostResponse.builder()
                .posts(List.of(DataUtil.getPostPreviewDto()))
                .hasPrev(false)
                .hasNext(false)
                .lastPage(1)
                .build();

        when(service.getPosts(search,pageNumber,pageSize)).thenReturn(example);

        mockMvc.perform(get("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("search",search)
                        .param("pageNumber",String.valueOf(pageNumber))
                        .param("pageSize",String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0]").value(example.getPosts().get(0)))
                .andExpect(jsonPath("$.hasPrev").value(example.isHasPrev()))
                .andExpect(jsonPath("$.hasNext").value(example.isHasNext()))
                .andExpect(jsonPath("$.lastPage").value(example.getLastPage()));
        verify(service).getPosts(search,pageNumber,pageSize);
    }

    @Test
    void getPostSuccess() throws Exception {
        Post example= DataUtil.getPostDto();

        when(service.getPost(example.getId())).thenReturn(example);

        mockMvc.perform(get("/api/posts/{id}",example.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(111))
                .andExpect(jsonPath("$.title").value("Java"))
                .andExpect(jsonPath("$.tags[0]").value("java"));

        verify(service,times(1)).getPost(example.getId());
    }

    @Test
    void getPostIdNotFound() throws Exception {
        when(service.getPost(999L)).thenThrow(new PostWithIdNotFound("Пост с таким ID не найден "));

        mockMvc.perform(get("/api/posts/{id}",999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Пост с таким ID не найден "));

        verify(service,times(1)).getPost(999L);
    }

    @Test
    void updatePost() throws Exception {
        UpdatePostRequest postRequest=DataUtil.getUpdatePost();
        PostPreviewDto example=DataUtil.getPostPreviewDto();
        String requestJson= """
                {
                    "id": 100,
                    "title": "New post",
                    "text": "Text posts",
                    "tags": ["spring","test"]
                }""";

        when(service.updatePost(postRequest.getId(),postRequest.getTitle(),postRequest.getText(),postRequest.getTags()))
                .thenReturn(example);

        mockMvc.perform(put("/api/posts/{id}",postRequest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(example.getId()))
                .andExpect(jsonPath("$.title").value(example.getTitle()))
                .andExpect(jsonPath("$.text").value(example.getText()))
                .andExpect(jsonPath("$.tags[0]").value(example.getTags().get(0)))
                .andExpect(jsonPath("$.tags[1]").value(example.getTags().get(1)));

        verify(service).updatePost(postRequest.getId(),postRequest.getTitle(),postRequest.getText(),postRequest.getTags());
    }

    @Test
    void updatePostIdNotFound() throws Exception {
        String requestJson= """
                {
                    "id": 999,
                    "title": "New post",
                    "text": "Text posts",
                    "tags": ["spring","test"]
                }""";

        when(service.updatePost(anyLong(),anyString(),anyString(),anyList())).thenThrow(new PostWithIdNotFound("Пост с таким ID не найден "));

        mockMvc.perform(put("/api/posts/{id}",999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Пост с таким ID не найден "));

        verify(service,times(1)).updatePost(anyLong(),anyString(),anyString(),anyList());
    }

    @Test
    void deletePostSuccess() throws Exception {
        Long id=999L;

        doNothing().when(service).deletePost(id);

        mockMvc.perform(delete("/api/posts/{id}",id))
                .andExpect(status().isOk());

        verify(service).deletePost(id);
    }

    @Test
    void deletePostIdNotFound() throws Exception {
        doThrow(new PostWithIdNotFound("Пост с таким ID не найден")).when(service).deletePost(999L);

        mockMvc.perform(delete("/api/posts/{id}",999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Пост с таким ID не найден "));

        verify(service,times(1)).deletePost(999L);
    }

    @Test
    void likesIncrementSuccess() throws Exception {
        Long id=999L;

        when(service.likesIncrement(id)).thenReturn(1);

        mockMvc.perform(post("/api/posts/{id}/likes",id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));

        verify(service).likesIncrement(id);
    }

    @Test
    void likesIncrementPostIdNotFound() throws Exception {
        when(service.likesIncrement(999L)).thenThrow(new PostWithIdNotFound("Пост с таким ID не найден "));

        mockMvc.perform(post("/api/posts/{id}/likes",999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Пост с таким ID не найден "));

        verify(service,times(1)).likesIncrement(999L);
    }

}