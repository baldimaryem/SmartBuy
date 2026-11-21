package tn.smartbuy.commun;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Oracle (avant 23ai) n'a pas de type booléen dans les tables : on stocke 'O' (oui) / 'N' (non)
 * dans une colonne CHAR(1). Ce convertisseur fait la traduction Java boolean <-> 'O'/'N'.
 */
@Converter
public class OuiNonConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean valeur) {
        return Boolean.TRUE.equals(valeur) ? "O" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String colonne) {
        return "O".equals(colonne);
    }
}
