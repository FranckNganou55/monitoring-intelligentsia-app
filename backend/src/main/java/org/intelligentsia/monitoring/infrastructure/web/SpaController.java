package org.intelligentsia.monitoring.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renvoie index.html pour toutes les routes React Router afin que le navigateur
 * puisse gérer la navigation côté client. Les routes /api/** et les fichiers
 * statiques (*.js, *.css…) sont gérés par leurs handlers respectifs.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/{path:[^\\.]*}",
        "/{path:[^\\.]*}/{sub1:[^\\.]*}",
        "/{path:[^\\.]*}/{sub1:[^\\.]*}/{sub2:[^\\.]*}",
    })
    public String spa() {
        return "forward:/index.html";
    }
}
