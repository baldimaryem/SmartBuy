package tn.smartbuy.liste;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListeCoursesRepository extends JpaRepository<ListeCourses, Long> {

    List<ListeCourses> findByUtilisateurIdOrderByDateCreationDescIdDesc(Long utilisateurId);
}
