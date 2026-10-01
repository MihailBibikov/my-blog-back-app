package ru.bibikov.myblogbackapp.exception.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.bibikov.myblogbackapp.exception.comment.CommentIdIsNull;
import ru.bibikov.myblogbackapp.exception.comment.CommentWithIdNotFound;
import ru.bibikov.myblogbackapp.exception.post.PostIdIsNull;
import ru.bibikov.myblogbackapp.exception.post.PostWithIdNotFound;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(PostWithIdNotFound.class)
    public ResponseEntity handlePostNotFound(PostWithIdNotFound e){
        log.warn("Пост не найден: {}",e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE,"text/plain;charset=UTF-8")
                .body("Пост с таким ID не найден ");
    }
    @ExceptionHandler(PostIdIsNull.class)
    public ResponseEntity handlePostIdIsNull(PostIdIsNull e){
        log.warn("ID поста равна нулю: {}",e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE,"text/plain;charset=UTF-8")
                .body("ID не может быть null");
    }
    @ExceptionHandler(CommentWithIdNotFound.class)
    public ResponseEntity handleCommentNotFound(CommentWithIdNotFound e){
        log.warn("Комментарий не найден: {}",e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE,"text/plain;charset=UTF-8")
                .body("Комментарий с таким ID не найден ");
    }
    @ExceptionHandler(CommentIdIsNull.class)
    public ResponseEntity handleCommentIdIsNull(CommentIdIsNull e){
        log.warn("ID комментария равна нулю: {}",e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE,"text/plain;charset=UTF-8")
                .body("ID не может быть null"+e);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity handleIllegalArgumentException(IllegalArgumentException e){
        log.warn("Ошибка валидации данных: {}",e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE,"text/plain;charset=UTF-8")
                .body("Не правильно набран контент");

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Exception e){
        log.error("Непредвиденная ошибка ",e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Внутренняя ошибка сервера");
    }
}
