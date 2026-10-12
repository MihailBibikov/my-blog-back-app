package ru.bibikov.myblogbackapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bibikov.myblogbackapp.dto.PostPreviewDto;
import ru.bibikov.myblogbackapp.dto.PostResponse;
import ru.bibikov.myblogbackapp.exception.post.PostIdIsNull;
import ru.bibikov.myblogbackapp.exception.post.PostWithIdNotFound;
import ru.bibikov.myblogbackapp.model.Post;
import ru.bibikov.myblogbackapp.model.Tag;
import ru.bibikov.myblogbackapp.repository.PostRepository;
import ru.bibikov.myblogbackapp.repository.TagRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository repository;
    private final TagRepository tagRepository;

    @Transactional
    public PostPreviewDto createPost(String title, String text, List<String> tags) {
        log.info("Начало создания поста: title='{}', text='{}', tags={}",title,text,tags);

        log.debug("Начата проверка тегов {} на наличие их в таблице",tags);
        Set<Tag> managedTags=new HashSet<>();
        for (String tagName:tags){
            Tag existingTag=tagRepository.findByName(tagName).orElse(null);
            if (existingTag!=null){
                managedTags.add(existingTag);
            }
            else {
                Tag tag=new Tag();
                tag.setName(tagName);
                managedTags.add(tag);
            }
        }
        log.debug("Проверка закончена. Итоговые теги для сохранения tags={}",managedTags);

        Post post=repository.save(Post.builder()
                .title(title)
                .text(text)
                .tags(managedTags)
                .build());
        log.debug("Создан пост с id={}",post.getId());
        Post dto=repository.findByIdWithTags(post.getId()).orElseThrow(()->new PostWithIdNotFound("Not found"));
        return PostPreviewDto.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .text(dto.getText())
                .tags(dto.getTags().stream().map(Tag::getName).toList())
                .commentsCount(dto.getCommentsCount())
                .likesCount(dto.getLikesCount())
                .build();
    }

    public PostResponse findAll(String search, int pageNumber, int pageSize){
        log.debug("Получение постов: search='{}', page_number={}, page_size={}",search,pageNumber,pageSize);
        Pageable pageable=PageRequest.of(pageNumber-1,pageSize,Sort.by("id").descending());

        Page<Post> posts;
        if (search == null || search.trim().isEmpty()) {
            posts=repository.findAllWithTags(pageable);
        } else {
            posts=repository.findByTitleContainingIgnoreCase(search,pageable);
        }

        Page<PostPreviewDto> dto=posts.map(this::mapToPreviewDto);
        return PostResponse.builder()
                .posts(dto.getContent())
                .hasPrev(dto.hasPrevious())
                .hasNext(dto.hasNext())
                .lastPage(dto.getTotalPages())
                .build();
    }

    private PostPreviewDto mapToPreviewDto(Post post) {
        List<String> tagNames=Optional.ofNullable(post.getTags())
                .orElse(Collections.emptySet())
                .stream()
                .map(Tag::getName)
                .toList();

        return PostPreviewDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .text(post.getText())
                .tags(tagNames)
                .commentsCount(post.getCommentsCount())
                .likesCount(post.getLikesCount())
                .build();
    }

    public Post findById(Long postId){
        validateId(postId);
        log.debug("Получение поста с post_id={}",postId);
        return repository.findByIdWithTags(postId).orElseThrow(() -> new PostWithIdNotFound("Post with this id not found"));
    }

    @Transactional
    public PostPreviewDto updatePost(Long id, String title, String text, List<String> tags) {
        validateId(id);
        log.info("Обновление поста с post_id={}: title='{}', text='{}', tags={}",id,title,text,tags);
        log.debug("Начата проверка тегов {} для поста с post_id={}",tags,id);
        Set<Tag> managedTags=new HashSet<>();
        for (String tagName:tags){
            Tag existingTag=tagRepository.findByName(tagName).orElse(null);
            if (existingTag!=null){
                managedTags.add(existingTag);
            }
            else {
                Tag tag=new Tag();
                tag.setName(tagName);
                managedTags.add(tag);
            }
        }

        Post post=Post.builder()
                .id(id)
                .title(title)
                .text(text)
                .tags(managedTags)
                .build();
        repository.save(post);
        log.debug("Обновление поста прошло успешно");
        Post dto=repository.findByIdWithTags(id).orElseThrow(()->new PostWithIdNotFound("Post with this id not found"));
        return PostPreviewDto.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .text(dto.getText())
                .tags(dto.getTags().stream().map(Tag::getName).toList())
                .likesCount(dto.getLikesCount())
                .commentsCount(dto.getCommentsCount())
                .build();
    }
    @Transactional
    public Integer likesIncrement(Long id) {
        validateId(id);
        log.debug("Инкрементация лайков с post_id={}",id);
        return repository.incrementLikes(id);
    }

    public void deletePost(Long id) {
        validateId(id);
        log.info("Удаление поста с post_id={}",id);
        repository.deleteById(id);
        log.info("Пост с post_id={} успешно удален",id);
    }
    private void validateId(Long postId){
        if (postId==null){
            throw new PostIdIsNull("ID поста равен null");
        }if (!repository.existsById(postId)){
            throw new PostWithIdNotFound("Пост с таким ID не найден");
        }
    }
}