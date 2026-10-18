package com.example.devcoast.service;

import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.devcoast.model.Empresa;
import com.example.devcoast.repository.EmpresaRepository;

@Service
public class PlanService {

    public static final String PLAN_GRATIS = "GRATIS";
    public static final String PLAN_PRO = "PRO";

    public static final int LIMITE_ACTIVAS_GRATIS = 2;
    public static final int LIMITE_ACTIVAS_PRO = 15;
    public static final int LIMITE_DESTACADOS_GRATIS = 0;
    public static final int LIMITE_DESTACADOS_PRO = 3;
    public static final double TARIFA_COLOCACION = 5.0;
    public static final int DURACION_PLAN_DIAS = 30;

    private final EmpresaRepository empresaRepository;

    public PlanService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public boolean esPro(Empresa empresa) {
        return empresa != null && PLAN_PRO.equals(empresa.getPlan());
    }

    public int limiteVacantesActivas(Empresa empresa) {
        return esPro(empresa) ? LIMITE_ACTIVAS_PRO : LIMITE_ACTIVAS_GRATIS;
    }

    public int limiteDestacados(Empresa empresa) {
        return esPro(empresa) ? LIMITE_DESTACADOS_PRO : LIMITE_DESTACADOS_GRATIS;
    }

    public void actualizarAPro(Empresa empresa) {
        empresa.setPlan(PLAN_PRO);
        empresa.setPlanVenceEn(sumarDias(new Date(), DURACION_PLAN_DIAS));
        empresaRepository.save(empresa);
    }

    private Date sumarDias(Date fecha, int dias) {
        return new Date(fecha.getTime() + dias * 24L * 60 * 60 * 1000);
    }

    // cada madrugada baja a plan gratis a las empresas cuyo plan PRO ya se venció
    @Scheduled(cron = "0 0 3 * * *")
    public void degradarPlanesVencidos() {
        Date ahora = new Date();
        List<Empresa> vencidas = empresaRepository.findByPlanAndPlanVenceEnBefore(PLAN_PRO, ahora);
        for (Empresa e : vencidas) {
            e.setPlan(PLAN_GRATIS);
            empresaRepository.save(e);
        }
    }
}
