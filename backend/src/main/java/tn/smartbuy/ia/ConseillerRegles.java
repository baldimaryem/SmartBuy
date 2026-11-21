package tn.smartbuy.ia;

import org.springframework.stereotype.Component;
import tn.smartbuy.ia.IaDtos.Conseil;
import tn.smartbuy.ia.IaDtos.Niveau;
import tn.smartbuy.ia.ResumeMois.DepenseCategorie;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Moteur de règles local (BUD-16) : simple, explicable, sans dépendance externe.
 * C'est aussi la solution de secours quand l'IA n'est pas configurée ou ne répond pas.
 */
@Component
public class ConseillerRegles implements ConseillerBudget {

    /** Hausse jugée importante par rapport à la moyenne des 3 mois précédents. */
    static final double HAUSSE_SIGNIFICATIVE = 0.30;
    static final double SEUIL_BUDGET = 80.0;
    static final double TAUX_EPARGNE_FAIBLE = 10.0;
    static final double TAUX_EPARGNE_BON = 20.0;

    @Override
    public List<Conseil> conseils(ResumeMois r) {
        List<Conseil> conseils = new ArrayList<>();
        String d = r.devise();

        // 1. Budget global : dépassé, ou dépassement probable d'après la prévision (BUD-15)
        if (r.budgetGlobal() != null) {
            if (r.depenses() >= r.budgetGlobal()) {
                conseils.add(new Conseil("Budget global dépassé",
                        "Vous avez dépensé " + m(r.depenses(), d) + " pour un budget de " + m(r.budgetGlobal(), d)
                                + ". Limitez les achats non essentiels jusqu'à la fin du mois.", Niveau.ALERTE));
            } else if (r.moisEnCours() && r.previsionFinMois() > r.budgetGlobal()) {
                conseils.add(new Conseil("Dépassement probable",
                        "À ce rythme, vos dépenses atteindront environ " + m(r.previsionFinMois(), d)
                                + " en fin de mois, soit " + m(r.previsionFinMois() - r.budgetGlobal(), d)
                                + " de plus que votre budget global.", Niveau.ALERTE));
            }
        }

        for (DepenseCategorie c : r.categories()) {
            // 2. Budget d'une catégorie à plus de 80 % (ou dépassé)
            if (c.budget() != null && c.budget() > 0) {
                double pourcentage = c.montant() / c.budget() * 100;
                if (pourcentage >= 100) {
                    conseils.add(new Conseil("Budget " + c.categorie() + " dépassé",
                            "Vous avez dépensé " + m(c.montant(), d) + " pour un budget de " + m(c.budget(), d) + ".",
                            Niveau.ALERTE));
                } else if (pourcentage >= SEUIL_BUDGET) {
                    conseils.add(new Conseil("Budget " + c.categorie() + " presque atteint",
                            Math.round(pourcentage) + " % du budget utilisé : il reste " + m(c.budget() - c.montant(), d)
                                    + ".", Niveau.ATTENTION));
                }
            }
            // 3. Hausse de plus de 30 % par rapport à la moyenne des 3 mois précédents
            if (c.moyenne3MoisPrecedents() > 0
                    && c.montant() > c.moyenne3MoisPrecedents() * (1 + HAUSSE_SIGNIFICATIVE)) {
                long hausse = Math.round((c.montant() / c.moyenne3MoisPrecedents() - 1) * 100);
                conseils.add(new Conseil("Hausse des dépenses " + c.categorie(),
                        "Vos dépenses " + c.categorie() + " ont augmenté de " + hausse
                                + " % par rapport à votre moyenne des 3 derniers mois ("
                                + m(c.moyenne3MoisPrecedents(), d) + ").", Niveau.ATTENTION));
            }
        }

        // 4. Taux d'épargne = (revenus − dépenses) / revenus
        if (r.revenus() > 0) {
            double taux = (r.revenus() - r.depenses()) / r.revenus() * 100;
            if (taux < 0) {
                conseils.add(new Conseil("Dépenses supérieures aux revenus",
                        "Ce mois-ci vous dépensez " + m(r.depenses() - r.revenus(), d) + " de plus que vos revenus.",
                        Niveau.ALERTE));
            } else if (taux < TAUX_EPARGNE_FAIBLE) {
                conseils.add(new Conseil("Épargne faible",
                        "Vous mettez de côté " + Math.round(taux) + " % de vos revenus. Visez au moins 10 %.",
                        Niveau.ATTENTION));
            } else if (taux >= TAUX_EPARGNE_BON) {
                conseils.add(new Conseil("Bonne gestion",
                        "Vous épargnez " + Math.round(taux) + " % de vos revenus ce mois-ci. Continuez ainsi !",
                        Niveau.INFO));
            }
        } else if (r.depenses() > 0) {
            conseils.add(new Conseil("Aucun revenu enregistré",
                    "Ajoutez vos revenus du mois pour suivre votre taux d'épargne.", Niveau.INFO));
        }

        // 5. Magasin le plus avantageux du catalogue
        if (r.magasinLeMoinsCher() != null && r.nbProduitsCompares() > 0) {
            conseils.add(new Conseil("Où faire vos courses ?",
                    r.magasinLeMoinsCher() + " est le moins cher sur " + r.nbProduitsMoinsChers() + " des "
                            + r.nbProduitsCompares() + " produits comparés. Comparez votre liste avant d'acheter.",
                    Niveau.INFO));
        }

        // 6. Économies déjà réalisées grâce à la comparaison (LNK-03)
        if (r.economiesRealisees() > 0) {
            conseils.add(new Conseil("Économies réalisées",
                    "La comparaison des prix vous a fait économiser " + m(r.economiesRealisees(), d) + " ce mois-ci.",
                    Niveau.INFO));
        }

        if (conseils.isEmpty()) {
            conseils.add(new Conseil("Rien à signaler",
                    "Vos dépenses sont maîtrisées. Définissez des budgets par catégorie pour un suivi plus précis.",
                    Niveau.INFO));
        }
        // Les plus importants d'abord : ALERTE, puis ATTENTION, puis INFO
        conseils.sort(Comparator.comparing((Conseil c) -> c.niveau().ordinal()).reversed());
        return conseils;
    }

    /** Assistant par mots-clés : reconnaît quelques questions fréquentes. */
    @Override
    public String repondre(String question, ResumeMois r) {
        String q = sansAccents(question);
        String d = r.devise();

        if (contient(q, "reste", "encore", "combien", "plafond")) {
            if (r.budgetGlobal() == null) {
                return "Vous n'avez pas de budget global pour " + r.mois() + ". Vous avez dépensé "
                        + m(r.depenses(), d) + " pour " + m(r.revenus(), d) + " de revenus.";
            }
            return "Il vous reste " + m(r.budgetGlobal() - r.depenses(), d) + " sur votre budget global de "
                    + m(r.budgetGlobal(), d) + " (déjà dépensé : " + m(r.depenses(), d) + ").";
        }
        if (contient(q, "prevision", "fin du mois", "fin de mois", "depasser", "rythme")) {
            String base = "À ce rythme, vos dépenses de fin de mois sont estimées à " + m(r.previsionFinMois(), d) + ".";
            if (r.budgetGlobal() != null) {
                base += r.previsionFinMois() > r.budgetGlobal()
                        ? " C'est plus que votre budget global (" + m(r.budgetGlobal(), d) + ") : ralentissez un peu."
                        : " Cela reste dans votre budget global (" + m(r.budgetGlobal(), d) + ").";
            }
            return base;
        }
        if (contient(q, "epargn", "economis", "mettre de cote")) {
            double solde = r.revenus() - r.depenses();
            return "Ce mois-ci, votre solde est de " + m(solde, d) + ". La comparaison des prix vous a fait économiser "
                    + m(r.economiesRealisees(), d) + ". Astuce : fixez un budget par catégorie et comparez vos listes de courses.";
        }
        if (contient(q, "magasin", "moins cher", "courses", "acheter")) {
            if (r.magasinLeMoinsCher() == null) {
                return "Pas encore assez de prix pour recommander un magasin.";
            }
            return r.magasinLeMoinsCher() + " est le moins cher sur " + r.nbProduitsMoinsChers() + " des "
                    + r.nbProduitsCompares() + " produits comparés. Utilisez la comparaison de votre liste pour le vérifier.";
        }
        if (contient(q, "categorie", "plus depense", "ou part", "poste")) {
            return r.categories().stream()
                    .max(Comparator.comparingDouble(DepenseCategorie::montant))
                    .map(c -> "Votre plus gros poste de dépense est « " + c.categorie() + " » avec " + m(c.montant(), d) + ".")
                    .orElse("Aucune dépense enregistrée pour " + r.mois() + ".");
        }
        return "Pour " + r.mois() + " : revenus " + m(r.revenus(), d) + ", dépenses " + m(r.depenses(), d)
                + ". Vous pouvez me demander ce qu'il vous reste, la prévision de fin de mois, "
                + "votre plus grosse catégorie de dépense ou le magasin le moins cher.";
    }

    private static boolean contient(String texte, String... motsCles) {
        for (String mot : motsCles) {
            if (texte.contains(mot)) {
                return true;
            }
        }
        return false;
    }

    /** « Épargne » -> « epargne » : la recherche de mots-clés ignore accents et majuscules. */
    static String sansAccents(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String m(double montant, String devise) {
        return String.format(Locale.FRANCE, "%.3f %s", montant, devise);
    }
}
