package tn.smartbuy.prix;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProduitRepository extends JpaRepository<Produit, Long> {

    /**
     * Recherche PRX-02 / PRX-03. Chaque filtre est facultatif : s'il vaut null, la condition est ignorée.
     * {@code texte} est déjà en minuscules : recherche insensible à la casse dans le nom ou la marque.
     * locate(a, b) &gt; 0 signifie « b contient a » (INSTR en Oracle) ; contrairement à LIKE,
     * un « % » ou un « _ » tapé par l'utilisateur n'a pas de sens spécial.
     */
    @Query("""
            select p from Produit p
            where (:texte is null or locate(:texte, lower(p.nom)) > 0 or locate(:texte, lower(p.marque)) > 0)
              and (:categorieId is null or p.categorieProduit.id = :categorieId)
              and (:codeBarres is null or p.codeBarres = :codeBarres)
              and (:univers is null or p.categorieProduit.univers = :univers)
            order by p.nom, p.marque
            """)
    List<Produit> rechercher(@Param("texte") String texte,
                             @Param("categorieId") Long categorieId,
                             @Param("codeBarres") String codeBarres,
                             @Param("univers") Univers univers);

    java.util.Optional<Produit> findFirstByCodeBarres(String codeBarres);

    java.util.Optional<Produit> findFirstByUrlSource(String urlSource);

    List<Produit> findByCodeBarresIsNotNull();

    boolean existsByCodeBarres(String codeBarres);

    boolean existsByCategorieProduitId(Long categorieProduitId);
}
