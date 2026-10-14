package org.intelligentsia.monitoring.domain.extraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Définition d'un paramètre attendu par un ExtractionTemplate.
 * Sérialisé en JSONB dans extraction_templates.parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterDef {
    /** Clé utilisée dans la requête SQL : :paramName */
    private String name;
    /** STRING | INTEGER | LONG | DECIMAL | DATE | DATETIME | BOOLEAN */
    private String type;
    /** Libellé affiché dans le formulaire frontend */
    private String label;
    private boolean required;
    private String defaultValue;
}
