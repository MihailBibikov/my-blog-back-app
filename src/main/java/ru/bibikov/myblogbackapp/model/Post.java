package ru.bibikov.myblogbackapp.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    private Long id;
    private String title;
    private String text;
    private int likesCount;
    private int commentsCount;
    private String image;
    private List<String> tags;
}
