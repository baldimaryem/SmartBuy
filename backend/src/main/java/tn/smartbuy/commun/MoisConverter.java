package tn.smartbuy.commun;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;

/** Stocke un mois Java (YearMonth) sous la forme texte 'AAAA-MM' dans une colonne VARCHAR2(7). */
@Converter
public class MoisConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth mois) {
        return mois == null ? null : mois.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String colonne) {
        return colonne == null ? null : YearMonth.parse(colonne);
    }
}
