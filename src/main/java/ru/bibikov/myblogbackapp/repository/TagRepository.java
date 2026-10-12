package ru.bibikov.myblogbackapp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.bibikov.myblogbackapp.model.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag,Long> {
    List<Tag> findByNameIn(Collection<String> names);

    Optional<Tag> findByName(String name);
}
