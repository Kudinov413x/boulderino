package boulderino.boulderino.service;

import boulderino.boulderino.dto.BoulderCreateDTO;
import boulderino.boulderino.dto.BoulderUpdateDTO;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.BoulderRepository;
import org.springframework.stereotype.Service;
import boulderino.boulderino.dto.BoulderResponseDTO;

import java.util.List;

@Service
public class BoulderService {

    private final BoulderRepository boulderRepository;

    public BoulderService(BoulderRepository boulderRepository) {
        this.boulderRepository = boulderRepository;
    }

    // Alle Boulder eines Users
    public List<BoulderResponseDTO> getAllBouldersByUser(User user) {
        return boulderRepository.findByUser(user)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // Boulder eines Users anhand der ID finden
    public BoulderResponseDTO getBoulderById(Long id, User user) {

        Boulder boulder =  boulderRepository.findById(id)
                .filter(b -> b.getUser().equals(user))
                .orElseThrow();

        return toResponseDTO(boulder);
    }

    // Neuen Boulder für einen User erstellen
    public Boulder createBoulder(BoulderCreateDTO request, User user) {

        Boulder boulder = new Boulder();

        boulder.setName(request.getName());
        boulder.setGrade(request.getGrade());
        boulder.setWallAngle(request.getWallAngle());
        boulder.setGripType(request.getGripType());
        boulder.setRouteCharacter(request.getRouteCharacter());
        boulder.setClimbingStyle(request.getClimbingStyle());
        boulder.setUser(user);

        return boulderRepository.save(boulder);
    }

    // Boulder eines Users ändern
    public BoulderResponseDTO updateBoulder(Long id,BoulderUpdateDTO request,User user) {
        Boulder existingBoulder = getBoulderEntityById(id, user);

        if (request.getName() != null && !request.getName().isBlank()) {
            existingBoulder.setName(request.getName());
        }

        if (request.getGrade() != null) {
            existingBoulder.setGrade(request.getGrade());
        }

        if (request.getWallAngle() != null) {
            existingBoulder.setWallAngle(request.getWallAngle());
        }

        if (request.getGripType() != null) {
            existingBoulder.setGripType(request.getGripType());
        }

        if (request.getRouteCharacter() != null) {
            existingBoulder.setRouteCharacter(request.getRouteCharacter());
        }

        if (request.getClimbingStyle() != null) {
            existingBoulder.setClimbingStyle(request.getClimbingStyle());
        }

        Boulder updatedBoulder = boulderRepository.save(existingBoulder);

        return toResponseDTO(updatedBoulder);
    }

    // Boulder eines Users löschen
    public void deleteBoulder(Long id, User user) {

        Boulder existingBoulder = getBoulderEntityById(id, user);

        boulderRepository.delete(existingBoulder);
    }

    // Hilfsmethode: Entity -> ResponseDTO
    private BoulderResponseDTO toResponseDTO(Boulder boulder) {

        BoulderResponseDTO response = new BoulderResponseDTO();

        response.setId(boulder.getId());
        response.setName(boulder.getName());
        response.setGrade(boulder.getGrade());
        response.setWallAngle(boulder.getWallAngle());
        response.setGripType(boulder.getGripType());
        response.setRouteCharacter(boulder.getRouteCharacter());
        response.setClimbingStyle(boulder.getClimbingStyle());

        return response;
    }

    // Hilfsmethode für Operationen, die die Entity benötigen
    private Boulder getBoulderEntityById(Long id, User user) {

        return boulderRepository.findById(id)
                .filter(boulder -> boulder.getUser().equals(user))
                .orElseThrow();
    }
}