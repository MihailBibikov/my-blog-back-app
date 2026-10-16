package ru.bibikov.myblogbackapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.bibikov.myblogbackapp.dto.PostPreviewDto;
import ru.bibikov.myblogbackapp.dto.PostResponse;
import ru.bibikov.myblogbackapp.exception.post.PostIdIsNull;
import ru.bibikov.myblogbackapp.exception.post.PostWithIdNotFound;
import ru.bibikov.myblogbackapp.model.Post;
import ru.bibikov.myblogbackapp.model.Tag;
import ru.bibikov.myblogbackapp.repository.PostRepository;
import ru.bibikov.myblogbackapp.repository.TagRepository;
import ru.bibikov.myblogbackapp.utils.DataUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository repository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private PostService service;

    @Test
    void createPostWithTagsSuccess() {
        Post post=DataUtil.getPostPersisted();
        when(repository.save(any(Post.class))).thenReturn(post);
        when(repository.findByIdWithTags(post.getId())).thenReturn(Optional.of(post));
        when(tagRepository.findByName("Java")).thenReturn(Optional.of(DataUtil.getTagJavaPersisted()));
        when(tagRepository.findByName("Equal")).thenReturn(Optional.of(DataUtil.getTagEqualPersisted()));

        PostPreviewDto rs=service.createPost(post.getTitle(), post.getText(), List.of("Java","Equal"));

        assertNotNull(rs);
        assertEquals(post.getTitle(),rs.getTitle());
        assertEquals(post.getText(),rs.getText());
        assertEquals(post.getTags().stream().map(Tag::getName).toList(),rs.getTags());
        verify(repository).save(any(Post.class));
        verify(repository).findByIdWithTags(post.getId());
        verify(tagRepository).findByName("Java");
        verify(tagRepository).findByName("Equal");
    }

    @Test
    void createPostWithoutTagsSuccess(){
        Post post =DataUtil.getPostPersisted();
        post.setTags(Set.of());

        when(repository.save(any(Post.class))).thenReturn(post);
        when(repository.findByIdWithTags(post.getId())).thenReturn(Optional.of(post));

        PostPreviewDto result=service.createPost(post.getTitle(),post.getText(),List.of());

        assertNotNull(result);
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());
        assertEquals(0,result.getTags().size());

        verify(repository).save(any(Post.class));
        verify(repository).findByIdWithTags(post.getId());

    }

    @Test
    void getPostsWithSearchParameterPagination() {
        Post post=DataUtil.getPostPersisted();
        Page<Post> page=new PageImpl<>(List.of(post));
        when(repository.findByTitleContainingIgnoreCase(anyString(),any(Pageable.class))).thenReturn(page);

        PostResponse response=service.findAll("Java",1,5);

        assertNotNull(response);
        assertEquals(1,response.getLastPage());
        assertFalse(response.isHasPrev());
        assertFalse(response.isHasNext());
        assertEquals(1,response.getPosts().size());
        assertEquals(post.getId(),response.getPosts().get(0).getId());

        verify(repository).findByTitleContainingIgnoreCase(anyString(),any(Pageable.class));
    }

    @Test
    void getPostsWithoutSearchParameterPagination(){
        Post post=DataUtil.getPostPersisted();
        Page<Post> page=new PageImpl<>(List.of(post));
        when(repository.findAllWithTags(any(Pageable.class))).thenReturn(page);

        PostResponse response=service.findAll("",1,5);

        assertNotNull(response);
        assertEquals(1,response.getLastPage());
        assertFalse(response.isHasPrev());
        assertFalse(response.isHasNext());
        assertEquals(1,response.getPosts().size());
        assertEquals(post.getId(),response.getPosts().get(0).getId());

        verify(repository).findAllWithTags(any(Pageable.class));
    }

    @Test
    void getPostsReturnHasNextFalse(){
        Post post=DataUtil.getPostPersisted();
        Pageable pageable= PageRequest.of(1,1);
        Page<Post> page=new PageImpl<>(List.of(post,Post.builder()
                .id(1L)
                .title("test")
                .text("text tests")
                .tags(Set.of())
                .likesCount(0)
                .commentsCount(0)
                .build()),pageable,2);
        when(repository.findAllWithTags(any(Pageable.class))).thenReturn(page);

        PostResponse response=service.findAll("",2,1);

        assertEquals(2,response.getLastPage());
        assertTrue(response.isHasPrev());
        assertFalse(response.isHasNext());

        verify(repository).findAllWithTags(any(Pageable.class));
    }

    @Test
    void getPostsReturnLastPageOne(){
        Post post=DataUtil.getPostPersisted();
        Page<Post> page=new PageImpl<>(List.of(post));
        when(repository.findAllWithTags(any(Pageable.class))).thenReturn(page);

        PostResponse response=service.findAll("",1,2);

        assertEquals(1,response.getLastPage());
        assertFalse(response.isHasPrev());
        assertFalse(response.isHasNext());

        verify(repository).findAllWithTags(any(Pageable.class));
    }

    @Test
    void getPostSuccess() {
        Post post=DataUtil.getPostPersisted();

        when(repository.existsById(anyLong())).thenReturn(true);
        when(repository.findByIdWithTags(anyLong())).thenReturn(Optional.of(post));


        Post result=service.findById(post.getId());

        assertNotNull(result);
        assertEquals(post.getId(),result.getId());
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());
        assertEquals(2,result.getTags().size());
        assertEquals(post.getTags(),result.getTags());

        verify(repository).findByIdWithTags(anyLong());
    }

    @Test
    void getPostWhereIdIsNull(){
        PostIdIsNull exception=assertThrows(PostIdIsNull.class,
                ()->service.findById(null));
        assertEquals("ID поста равен null",exception.getMessage());
        verify(repository,never()).findByIdWithTags(null);
    }

    @Test
    void getPostWhereIdNotFound(){
        when(repository.existsById(anyLong())).thenReturn(false);

        PostWithIdNotFound exception=assertThrows(PostWithIdNotFound.class,
                ()->service.findById(1L));
        assertEquals("Пост с таким ID не найден",exception.getMessage());
        verify(repository,never()).findByIdWithTags(1L);

    }

    @Test
    void likesIncrementSuccess() {
        long id=1L;
        when(repository.existsById(id)).thenReturn(true);
        when(repository.incrementLikes(id)).thenReturn(2);

        Integer result=service.likesIncrement(id);

        assertNotNull(result);
        assertEquals(2,result);

        verify(repository).existsById(id);
        verify(repository).incrementLikes(id);
    }

    @Test
    void likesIncrementIdIsNull() {
        PostIdIsNull exception=assertThrows(PostIdIsNull.class,
                ()->service.likesIncrement(null));
        assertEquals("ID поста равен null",exception.getMessage());
        verify(repository,never()).incrementLikes(anyLong());
    }

    @Test
    void likesIncrementIdNotFound(){
        when(repository.existsById(anyLong())).thenReturn(false);

        PostWithIdNotFound exception=assertThrows(PostWithIdNotFound.class,
                ()->service.likesIncrement(1L));
        assertEquals("Пост с таким ID не найден",exception.getMessage());
        verify(repository,never()).incrementLikes(1L);
    }

    @Test
    void updatePostWithTagsSuccess() {
        Post post= DataUtil.getPostPersisted();
        Tag tagJava=DataUtil.getTagJavaPersisted();
        Tag tagEqual=DataUtil.getTagEqualPersisted();

        when(repository.existsById(anyLong())).thenReturn(true);
        when(tagRepository.findByName(tagJava.getName())).thenReturn(Optional.of(tagJava));
        when(tagRepository.findByName(tagEqual.getName())).thenReturn(Optional.of(tagEqual));
        when(repository.save(any(Post.class))).thenReturn(post);
        when(repository.findByIdWithTags(anyLong())).thenReturn(Optional.ofNullable(post));

        PostPreviewDto result= service.updatePost(post.getId(), post.getTitle(),post.getText(),post.getTags().stream().map(Tag::getName).toList());

        assertNotNull(result);
        assertEquals(post.getId(),result.getId());
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());
        assertEquals(2,result.getTags().size());
        assertTrue(result.getTags().contains("Java"));
        assertTrue(result.getTags().contains("Equal"));

        verify(repository).existsById(anyLong());
        verify(tagRepository,times(2)).findByName(anyString());
        verify(repository).save(any(Post.class));
        verify(repository).findByIdWithTags(anyLong());
    }

    @Test
    void updatePostWithoutTagsSuccess(){
        Post post =DataUtil.getPostPersistedWithoutTags();

        when(repository.existsById(anyLong())).thenReturn(true);
        when(repository.save(any(Post.class))).thenReturn(post);
        when(repository.findByIdWithTags(anyLong())).thenReturn(Optional.ofNullable(post));

        PostPreviewDto result=service.updatePost(post.getId(),post.getTitle(),post.getText(),List.of());

        assertNotNull(result);
        assertEquals(post.getId(),result.getId());
        assertEquals(post.getTitle(),result.getTitle());
        assertEquals(post.getText(),result.getText());

        verify(repository).existsById(anyLong());
        verify(repository).save(any(Post.class));
        verify(repository).findByIdWithTags(anyLong());
    }

    @Test
    void updatePostIdIsNull(){
        PostIdIsNull exception=assertThrows(PostIdIsNull.class,
                ()->service.updatePost(null,"Hello","Just a text",null));
        assertEquals("ID поста равен null",exception.getMessage());
        verify(repository,never()).existsById(null);
        verify(repository,never()).save(any(Post.class));
        verify(repository,never()).findByIdWithTags(anyLong());
    }
    @Test
    void updatePostIdNotFound(){
        when(repository.existsById(anyLong())).thenReturn(false);

        PostWithIdNotFound exception=assertThrows(PostWithIdNotFound.class,
                ()->service.updatePost(1L,"Hello","Just a text",null));
        assertEquals("Пост с таким ID не найден",exception.getMessage());
        verify(repository).existsById(anyLong());
        verify(repository,never()).save(any(Post.class));
        verify(repository,never()).findByIdWithTags(anyLong());
    }

    @Test
    void deletePostSuccess() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        service.deletePost(1L);

        verify(repository).existsById(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void deletePostIdIsNull(){
        PostIdIsNull exception=assertThrows(PostIdIsNull.class,
                ()->service.deletePost(null));
        assertEquals("ID поста равен null",exception.getMessage());

        verify(repository,never()).deleteById(null);
    }
    @Test
    void deletePostIdNotFound(){
        when(repository.existsById(1L)).thenReturn(false);

        PostWithIdNotFound exception=assertThrows(PostWithIdNotFound.class,
                ()->service.deletePost(1L));
        assertEquals("Пост с таким ID не найден",exception.getMessage());
        verify(repository,never()).deleteById(1L);
    }

}