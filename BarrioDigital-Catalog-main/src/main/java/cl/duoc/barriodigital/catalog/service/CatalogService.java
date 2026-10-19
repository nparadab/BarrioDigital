package cl.duoc.barriodigital.catalog.service;

import cl.duoc.barriodigital.catalog.model.ProcedureType;
import cl.duoc.barriodigital.catalog.repository.ProcedureTypeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final ProcedureTypeRepository repository;

    public List<ProcedureType> getAllProcedures() {
        return repository.findAll();
    }

    public Optional<ProcedureType> getProcedureById(Long id) {
        return repository.findById(id);
    }

    public Optional<ProcedureType> getProcedureByCode(String code) {
        return repository.findByCode(code);
    }

    @Transactional
    public ProcedureType createProcedure(ProcedureType procedure) {
        if (procedure.getAvailableQuota() == null) {
            procedure.setAvailableQuota(procedure.getDailyQuota());
        }
        if (procedure.getActive() == null) {
            procedure.setActive(true);
        }
        return repository.save(procedure);
    }

    @Transactional
    public ProcedureType updateProcedure(Long id, ProcedureType updated) {
        ProcedureType existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de trámite no encontrado con ID: " + id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setDepartment(updated.getDepartment());
        existing.setDailyQuota(updated.getDailyQuota());
        existing.setAvailableQuota(updated.getAvailableQuota());
        existing.setRequirements(updated.getRequirements());
        if (updated.getActive() != null) {
            existing.setActive(updated.getActive());
        }

        return repository.save(existing);
    }

    @Transactional
    public synchronized ProcedureType decrementQuota(Long id) {
        ProcedureType procedure = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de trámite no encontrado con ID: " + id));

        if (procedure.getAvailableQuota() <= 0) {
            throw new IllegalStateException("No hay cupos diarios disponibles para el trámite: " + procedure.getName());
        }

        procedure.setAvailableQuota(procedure.getAvailableQuota() - 1);
        log.info("Cupo descontado para '{}' (ID: {}). Cupos restantes: {}", procedure.getName(), id, procedure.getAvailableQuota());
        return repository.save(procedure);
    }

    @Transactional
    public synchronized ProcedureType restoreQuota(Long id) {
        ProcedureType procedure = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de trámite no encontrado con ID: " + id));

        if (procedure.getAvailableQuota() < procedure.getDailyQuota()) {
            procedure.setAvailableQuota(procedure.getAvailableQuota() + 1);
        }
        return repository.save(procedure);
    }

    @Transactional
    public void deleteProcedure(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Tipo de trámite no encontrado con ID: " + id);
        }
        repository.deleteById(id);
        log.info("Tipo de trámite con ID {} eliminado correctamente", id);
    }


    @PostConstruct
    public void initData() {
        if (repository.count() == 0) {
            log.info("Inicializando catálogo comunal BarrioDigital...");
            repository.save(ProcedureType.builder()
                    .code("LUM-01")
                    .name("Reparación de Luminarias Públicas")
                    .description("Atención de focos apagados, intermitentes o postes dañados en la vía pública.")
                    .department("Alumbrado Público")
                    .dailyQuota(15)
                    .availableQuota(15)
                    .requirements("Indicar dirección exacta y número de poste si es visible.")
                    .active(true)
                    .build());

            repository.save(ProcedureType.builder()
                    .code("PAV-02")
                    .name("Bacheo y Reparación de Calzadas")
                    .description("Reparación de eventos, hoyos o baches en calzadas y veredas comunales.")
                    .department("Obras Comunales")
                    .dailyQuota(10)
                    .availableQuota(10)
                    .requirements("Fotografía referencial y descripción de la vía afectada.")
                    .active(true)
                    .build());

            repository.save(ProcedureType.builder()
                    .code("POD-03")
                    .name("Poda de Árboles en Vía Pública")
                    .description("Poda preventiva de ramas que obstruyan tendido eléctrico, señalética o techumbres.")
                    .department("Aseo y Ornato")
                    .dailyQuota(12)
                    .availableQuota(12)
                    .requirements("Ubicación en bien nacional de uso público (no patios privados).")
                    .active(true)
                    .build());

            repository.save(ProcedureType.builder()
                    .code("ESC-04")
                    .name("Retiro de Escombros y Voluminosos")
                    .description("Retiro programado de enseres domésticos en desuso, ramas o escombros menores.")
                    .department("Medio Ambiente")
                    .dailyQuota(20)
                    .availableQuota(20)
                    .requirements("Máximo 1 m3 por vivienda. Disponer en acera la víspera del retiro.")
                    .active(true)
                    .build());
        }
    }
}
