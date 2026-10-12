package ru.bibikov.myblogbackapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bibikov.myblogbackapp.exception.comment.CommentIdIsNull;
import ru.bibikov.myblogbackapp.exception.comment.CommentWithIdNotFound;
import ru.bibikov.myblogbackapp.exception.post.PostIdIsNull;
import ru.bibikov.myblogbackapp.exception.post.PostWithIdNotFound;
import ru.bibikov.myblogbackapp.model.Comment;
import ru.bibikov.myblogbackapp.model.Post;
import ru.bibikov.myblogbackapp.repository.CommentRepository;
import ru.bibikov.myblogbackapp.repository.PostRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository repository;
    private final PostRepository postRepository;

    @Transactional
    public Comment createComment(Long postId, String text) {
        validatePostId(postId);
        log.info("Создание комментария с post_id={}: comment='{}'",postId,text);
        Post post=postRepository.findById(postId).orElseThrow(()->new PostWithIdNotFound("Post not found"));
        post.setCommentsCount(post.getCommentsCount()+1);
        return repository.save(Comment.builder()
                .text(text)
                .post(post)
                .build());
    }


    public List<Comment> getComments(Long postId) {
        validatePostId(postId);
        log.debug("Получение комментариев с post_id={}",postId);
        return repository.findAllByPost_Id(postId);
    }

    public Comment getComment(Long commentId, Long postId) {
        validateId(commentId);
        validatePostId(postId);
        log.debug("Получение комментария с comment_id={} и post_id={}",commentId,postId);
        return repository.findByIdAndPost_Id(commentId,postId);
    }
    @Transactional
    public Comment updateComment(Long commentId, Long postId, String text) {
        validateId(commentId);
        validatePostId(postId);
        log.info("Обновление комментария с comment_id={} и post_id={}: text='{}'",commentId,postId,text);
        Comment comment=repository.findById(commentId).orElseThrow(()->new CommentWithIdNotFound("Comment with id="+commentId+" not found"));
        if (!comment.getPost().getId().equals(postId)){
            log.warn("Несовпадение ID! Комментраий {} принадлежит другому посту {}, а запрос пришел для поста {}",
                    commentId,comment.getPost().getId(),postId);
        }
        comment.setText(text);
        return repository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long postId) {
        validateId(commentId);
        validatePostId(postId);
        log.info("Удаление комментария с comment_id={} и post_id={}",commentId,postId);
        repository.deleteById(commentId);
        Post post=postRepository.findById(postId).orElseThrow(()->new PostWithIdNotFound("Post not found"));
        post.setCommentsCount(post.getCommentsCount()-1);
        log.info("Комментарий с comment_id={} и post_id={} успешно удален",commentId,postId);
    }

    private void validateId(Long commentId){
        if (commentId==null){
            throw new CommentIdIsNull("ID комментария равен нулю");
        }if (!repository.existsById(commentId)){
            throw new CommentWithIdNotFound("Комментарий с таким ID не найден");
        }
    }
    private void validatePostId(Long id){
        if (id==null){
            throw new PostIdIsNull("ID поста равен нулю");
        }if (!postRepository.existsById(id)){
            throw new PostWithIdNotFound("Пост с таким ID не найден");
        }
    }
}