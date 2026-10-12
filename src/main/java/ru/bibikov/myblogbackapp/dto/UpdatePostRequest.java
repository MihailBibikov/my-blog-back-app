package ru.bibikov.myblogbackapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bibikov.myblogbackapp.model.Tag;


import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    private Long id;
    private String title;
    private String text;
    private List<String> tags;

}