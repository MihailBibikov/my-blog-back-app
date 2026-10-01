package ru.bibikov.myblogbackapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import ru.bibikov.myblogbackapp.dto.PostPreviewDto;
import ru.bibikov.myblogbackapp.dto.PostResponse;
import ru.bibikov.myblogbackapp.exception.post.PostIdIsNull;
import ru.bibikov.myblogbackapp.exception.post.PostWithIdNotFound;
import ru.bibikov.myblogbackapp.model.Post;
import ru.bibikov.myblogbackapp.model.Tag;
import ru.bibikov.myblogbackapp.repository.PostRepository;
import ru.bibikov.myblogbackapp.utils.DataUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository repository;

    @InjectMocks
    private PostService service;

    @Test
    void createPostWithTagsSuccess() {
        Post post= DataUtil.getPostPersisted();
        List<Tag> tag= List.of(DataUtil.getTagJavaPersisted(),DataUtil.getTagEqualPersisted());
        List<Tag> existingTagsFromDb=List.of(DataUtil.getTagJavaPersisted());
        when(repository.createPostReturnId(post.getTitle(),post.getText())).thenReturn(post.getId());

        when(repository.existId(post.getId())).thenReturn(true);
        doNothing().when(repository).deletePostTag(post.getId());
        when(repository.tagsExist(anyString(),eq(tag.stream().map(Tag::getName).toList()))).thenReturn(existingTagsFromDb);
        when(repository.createTag(tag.get(1).getName())).thenReturn(tag.get(1).getId());
        doNothing().when(repository).createPostTagChain(eq(post.getId()),anyLong());
        when(repository.getPostWithId(post.getId())).thenReturn(post);
        when(repository.getTags(post.getId())).thenReturn(tag.stream().map(Tag::getName).toList());

        PostPreviewDto result= service.createPost(post.getTitle(),post.getText(),tag.stream().map(Tag::getName).toList());

        assertNotNull(result);
        assertEquals(post.getId(),result.getId());
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());
        assertEquals(2,result.getTags().size());
        assertTrue(result.getTags().contains("Java"));
        assertTrue(result.getTags().contains("Equal"));

        verify(repository).createPostReturnId(post.getTitle(),post.getText());
        verify(repository).existId(post.getId());
        verify(repository).deletePostTag(post.getId());
        verify(repository).tagsExist(anyString(),eq(tag.stream().map(Tag::getName).toList()));
        verify(repository,times(1)).createTag(tag.get(1).getName());
        verify(repository,times(2)).createPostTagChain(eq(post.getId()),anyLong());
        verify(repository).getPostWithId(post.getId());
        verify(repository).getTags(post.getId());
    }

    @Test
    void createPostWithoutTagsSuccess(){
        Post post =DataUtil.getPostPersisted();

        when(repository.createPostReturnId(post.getTitle(),post.getText())).thenReturn(post.getId());
        when(repository.existId(post.getId())).thenReturn(true);
        doNothing().when(repository).deletePostTag(post.getId());
        when(repository.getPostWithId(post.getId())).thenReturn(post);

        PostPreviewDto result=service.createPost(post.getTitle(),post.getText(),List.of());

        assertNotNull(result);
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());
        assertEquals(0,result.getTags().size());

        verify(repository).createPostReturnId(post.getTitle(),post.getText());
        verify(repository).existId(post.getId());
        verify(repository).deletePostTag(post.getId());
        verify(repository).getPostWithId(post.getId());
    }

    @Test
    void getPostsReturnCorrectPagination() {
        Post post=DataUtil.getPostPersisted();
        List<PostPreviewDto>posts=List.of(PostPreviewDto.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .text(post.getText())
                        .likesCount(post.getLikesCount())
                        .commentsCount(post.getCommentsCount())
                        .tags(post.getTags())
                        .build());
        when(repository.countPosts("%Hello%")).thenReturn(5);
        when(repository.findPostPaginated("%Hello%",2,0)).thenReturn(posts);

        PostResponse response=service.getPosts("Hello",1,2);

        assertNotNull(response);
        assertEquals(3,response.getLastPage());
        assertFalse(response.isHasPrev());
        assertTrue(response.isHasNext());
        assertEquals(1,response.getPosts().size());
        assertEquals(post.getId(),response.getPosts().get(0).getId());

        verify(repository).countPosts("%Hello%");
        verify(repository).findPostPaginated("%Hello%",2,0);
    }

    @Test
    void getPostsReturnHasNextFalse(){
        when(repository.countPosts("%Hello%")).thenReturn(5);
        when(repository.findPostPaginated("%Hello%",2,4)).thenReturn(List.of());

        PostResponse response=service.getPosts("Hello",3,2);

        assertEquals(3,response.getLastPage());
        assertTrue(response.isHasPrev());
        assertFalse(response.isHasNext());

        verify(repository).countPosts("%Hello%");
        verify(repository).findPostPaginated("%Hello%",2,4);
    }

    @Test
    void getPostsReturnLastPageOne(){
        when(repository.countPosts("%Hello%")).thenReturn(0);
        when(repository.findPostPaginated("%Hello%",2,0)).thenReturn(List.of());

        PostResponse response=service.getPosts("Hello",1,2);

        assertEquals(1,response.getLastPage());
        assertFalse(response.isHasPrev());
        assertFalse(response.isHasNext());
        assertTrue(response.getPosts().isEmpty());

        verify(repository).countPosts("%Hello%");
        verify(repository).findPostPaginated("%Hello%",2,0);
    }

    @Test
    void getPostSuccess() {
        Post post=DataUtil.getPostPersisted();
        Tag tag=DataUtil.getTagJavaPersisted();

        when(repository.existId(post.getId())).thenReturn(true);
        when(repository.getPostWithId(post.getId())).thenReturn(post);
        when(repository.getTags(post.getId())).thenReturn(List.of(tag.getName()));

        Post result=service.getPost(post.getId());

        assertNotNull(result);
        assertEquals(post.getId(),result.getId());
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());
        assertEquals(1,result.getTags().size());
        assertTrue(result.getTags().contains("Java"));

        verify(repository).existId(post.getId());
        verify(repository).getPostWithId(post.getId());
        verify(repository).getTags(post.getId());
    }

    @Test
    void getPostWhereIdIsNull(){
        //when(repository.existId(anyLong())).thenReturn(false);

        PostIdIsNull exception=assertThrows(PostIdIsNull.class,
                ()->service.getPost(null));
        assertEquals("ID поста равен null",exception.getMessage());
        verify(repository,never()).getPostWithId(null);
        verify(repository, never()).getTags(null);
    }

    @Test
    void getPostWhereIdNotFound(){
        when(repository.existId(anyLong())).thenReturn(false);

        PostWithIdNotFound exception=assertThrows(PostWithIdNotFound.class,
                ()->service.getPost(1L));
        assertEquals("Пост с таким ID не найден",exception.getMessage());
        verify(repository,never()).getPostWithId(1L);
        verify(repository, never()).getTags(1L);
    }

    @Test
    void likesIncrementSuccess() {
        long id=1L;
        when(repository.existId(id)).thenReturn(true);
        when(repository.likesIncrement(id)).thenReturn(2);

        Integer result=service.likesIncrement(id);

        assertNotNull(result);
        assertEquals(2,result);

        verify(repository).likesIncrement(id);
        verify(repository).existId(id);
    }

    @Test
    void likesIncrementIdIsNull() {
        PostIdIsNull exception=assertThrows(PostIdIsNull.class,
                ()->service.likesIncrement(null));
        assertEquals("ID поста равен null",exception.getMessage());
        verify(repository,never()).likesIncrement(anyLong());
    }

    @Test
    void likesIncrementIdNotFound(){
        when(repository.existId(anyLong())).thenReturn(false);

        PostWithIdNotFound exception=assertThrows(PostWithIdNotFound.class,
                ()->service.likesIncrement(1L));
        assertEquals("Пост с таким ID не найден",exception.getMessage());
        verify(repository,never()).likesIncrement(1L);
    }

    @Test
    void updatePostWithTagsSuccess() {
        Post post= DataUtil.getPostPersisted();
        List<Tag> tag= List.of(DataUtil.getTagJavaPersisted(),DataUtil.getTagEqualPersisted());
        List<Tag> existingTagsFromDb=List.of(DataUtil.getTagJavaPersisted());
        when(repository.existId(post.getId())).thenReturn(true);
        doNothing().when(repository).updatePost(post.getId(),post.getTitle(),post.getText());

        doNothing().when(repository).deletePostTag(post.getId());
        when(repository.tagsExist(anyString(),eq(tag.stream().map(Tag::getName).toList()))).thenReturn(existingTagsFromDb);
        when(repository.createTag(tag.get(1).getName())).thenReturn(tag.get(1).getId());
        doNothing().when(repository).createPostTagChain(eq(post.getId()),anyLong());
        when(repository.getPostWithId(post.getId())).thenReturn(post);
        when(repository.getTags(post.getId())).thenReturn(tag.stream().map(Tag::getName).toList());

        PostPreviewDto result= service.updatePost(post.getId(), post.getTitle(),post.getText(),tag.stream().map(Tag::getName).toList());

        assertNotNull(result);
        assertEquals(post.getId(),result.getId());
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());
        assertEquals(2,result.getTags().size());
        assertTrue(result.getTags().contains("Java"));
        assertTrue(result.getTags().contains("Equal"));

        verify(repository).updatePost(post.getId(),post.getTitle(),post.getText());
        verify(repository,times(2)).existId(post.getId());
        verify(repository).deletePostTag(post.getId());
        verify(repository).tagsExist(anyString(),eq(tag.stream().map(Tag::getName).toList()));
        verify(repository,times(1)).createTag(tag.get(1).getName());
        verify(repository,times(2)).createPostTagChain(eq(post.getId()),anyLong());
        verify(repository).getPostWithId(post.getId());
        verify(repository).getTags(post.getId());
    }

    @Test
    void updatePostWithoutTagsSuccess(){
        Post post =DataUtil.getPostPersisted();

        doNothing().when(repository).updatePost(post.getId(),post.getTitle(),post.getText());
        when(repository.existId(post.getId())).thenReturn(true);
        doNothing().when(repository).deletePostTag(post.getId());
        when(repository.getPostWithId(post.getId())).thenReturn(post);

        PostPreviewDto result=service.updatePost(post.getId(),post.getTitle(),post.getText(),List.of());

        assertNotNull(result);
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());
        assertEquals(0,result.getTags().size());

        verify(repository).updatePost(post.getId(),post.getTitle(),post.getText());
        verify(repository,times(2)).existId(post.getId());
        verify(repository).deletePostTag(post.getId());
        verify(repository).getPostWithId(post.getId());
    }

    @Test
    void updatePostIdIsNull(){
        PostIdIsNull exception=assertThrows(PostIdIsNull.class,
                ()->service.updatePost(null,"Hello","Just a text",null));
        assertEquals("ID поста равен null",exception.getMessage());
        verify(repository,never()).updatePost(null,"Hello","Just a text");
        verify(repository,never()).existId(null);
        verify(repository,never()).deletePostTag(null);
        verify(repository,never()).getPostWithId(null);
    }
    @Test
    void updatePostIdNotFound(){
        when(repository.existId(anyLong())).thenReturn(false);

        PostWithIdNotFound exception=assertThrows(PostWithIdNotFound.class,
                ()->service.updatePost(1L,"Hello","Just a text",null));
        assertEquals("Пост с таким ID не найден",exception.getMessage());
        verify(repository,never()).updatePost(null,"Hello","Just a text");
        verify(repository,never()).existId(null);
        verify(repository,never()).deletePostTag(null);
        verify(repository,never()).getPostWithId(null);
    }

    @Test
    void deletePostSuccess() {
        when(repository.existId(1L)).thenReturn(true);
        when(repository.deletePost(1L)).thenReturn(1);

        service.deletePost(1L);

        verify(repository).existId(1L);
        verify(repository).deletePost(1L);
    }

    @Test
    void deletePostIdIsNull(){
        PostIdIsNull exception=assertThrows(PostIdIsNull.class,
                ()->service.deletePost(null));
        assertEquals("ID поста равен null",exception.getMessage());

        verify(repository,never()).deletePost(null);
    }
    @Test
    void deletePostIdNotFound(){
        when(repository.existId(1L)).thenReturn(false);

        PostWithIdNotFound exception=assertThrows(PostWithIdNotFound.class,
                ()->service.deletePost(1L));
        assertEquals("Пост с таким ID не найден",exception.getMessage());
        verify(repository,never()).deletePost(1L);
    }
}