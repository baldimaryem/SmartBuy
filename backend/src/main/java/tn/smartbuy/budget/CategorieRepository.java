package tn.smartbuy.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    /** Catégories par défaut (utilisateur_id IS NULL) + celles de l'utilisateur. */
    List<Categorie> findByUtilisateurIdIsNullOrUtilisateurId(Long utilisateurId);

    /** Ex. la catégorie par défaut « Alimentation » de type DEPENSE (utilisée par les listes de courses). */
    Optional<Categorie> findFirstByNomAndTypeAndUtilisateurIdIsNull(String nom, TypeOperation type);
}
