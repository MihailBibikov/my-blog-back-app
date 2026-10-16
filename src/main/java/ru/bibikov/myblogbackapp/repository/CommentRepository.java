package ru.bibikov.myblogbackapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bibikov.myblogbackapp.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment,Long> {
    List<Comment> findAllByPost_Id(Long postId);

    Comment findByIdAndPost_Id(Long id, Long postId);
}