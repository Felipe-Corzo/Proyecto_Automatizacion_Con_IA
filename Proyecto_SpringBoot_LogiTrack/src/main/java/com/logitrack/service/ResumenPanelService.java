package com.logitrack.service;

import com.logitrack.dto.ResumenPanelDTO;
import com.logitrack.model.ResumenPanel;

public interface ResumenPanelService {

    ResumenPanel publicarResumen(ResumenPanelDTO resumenDTO);

    ResumenPanel obtenerUltimoResumen();
}
