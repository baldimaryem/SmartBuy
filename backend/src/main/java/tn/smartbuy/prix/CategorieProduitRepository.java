package tn.smartbuy.prix;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategorieProduitRepository extends JpaRepository<CategorieProduit, Long> {

    List<CategorieProduit> findAllByOrderByNomAsc();

    List<CategorieProduit> findByUniversOrderByNomAsc(Univers univers);

    java.util.Optional<CategorieProduit> findFirstByNom(String nom);

    boolean existsByNomIgnoreCase(String nom);
}
