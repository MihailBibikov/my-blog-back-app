package ru.bibikov.myblogbackapp.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Builder
@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name="posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title")
    private String title;
    @Column(name="text")
    private String text;
    @Column(name = "likes_count")
    private int likesCount;
    @Column(name = "comments_count")
    private int commentsCount;
    @Column(name = "image")
    private String image;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Tag> tags=new HashSet<>();

    @Builder
    public Post(Long id, String title, String text, int likesCount, int commentsCount, String image, Set<Tag> tags) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.image = image;
        this.tags = tags !=null?tags:new HashSet<>();
    }
}
