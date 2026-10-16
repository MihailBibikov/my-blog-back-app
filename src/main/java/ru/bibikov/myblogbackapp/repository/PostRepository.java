package ru.bibikov.myblogbackapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.bibikov.myblogbackapp.model.Post;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post,Long> {

   int countByTitleContainingIgnoreCase(String title);

   @Query("select p from Post p left join fetch p.tags where p.id= :id")
   Optional<Post> findByIdWithTags(@Param("id")Long id);

   @Modifying
   @Query("update Post p set p.likesCount=p.likesCount+1 where p.id=:id")
   Integer incrementLikes(@Param("id")Long id);

   @Query("select distinct p from Post p left join fetch p.tags where upper(p.title) like upper(concat('%',:search,'%') ) ")
   Page<Post> findByTitleContainingIgnoreCase(@Param("search")String title, Pageable pageable);

   @Query("select distinct p from Post p left join fetch p.tags")
   Page<Post> findAllWithTags(Pageable pageable);

}