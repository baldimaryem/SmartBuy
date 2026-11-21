package tn.smartbuy.prix;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MagasinRepository extends JpaRepository<Magasin, Long> {

    List<Magasin> findAllByOrderByNomAsc();

    /** Le magasin « en ligne » d'une enseigne (ex. Carrefour Tunisie), où sont rangés les prix collectés. */
    java.util.Optional<Magasin> findFirstByEnseigneAndEnLigneTrue(String enseigne);
}
