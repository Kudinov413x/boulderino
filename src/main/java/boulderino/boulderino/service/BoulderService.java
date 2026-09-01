package boulderino.boulderino.service;

import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.BoulderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoulderService {

    private final BoulderRepository boulderRepository;

    public BoulderService(BoulderRepository boulderRepository) {
        this.boulderRepository = boulderRepository;
    }

    // Alle Boulder in der DB ausgeben
    public List<Boulder> getAllBoulders() {
        return boulderRepository.findAll();
    }
    
    // Boulder durch seine ID finden
    public Boulder getBoulderById(Long id){
        return boulderRepository.findById(id).orElseThrow();
    }

    // Neuen Boulder einspeichern
    public Boulder createBoulder(Boulder boulder) {
        return boulderRepository.save(boulder);
    }

    // Einen existierenden Boulder anpassen
    public Boulder updateBoulder(Long id, Boulder boulder) {
        Boulder existingBoulder = boulderRepository.findById(id).orElseThrow();
        existingBoulder.setName(boulder.getName());
        existingBoulder.setGrade(boulder.getGrade());
        existingBoulder.setUser(boulder.getUser());
        return boulderRepository.save(existingBoulder);
    }

    // einen Boulder löschen
    public void deleteBoulder(Long id) {
        boulderRepository.deleteById(id);
    }

    // alle boulder eines Users ausgeben
    public List<Boulder> getAllBouldersByUser(User user){
        return boulderRepository.findByUser(user);
    }
}
