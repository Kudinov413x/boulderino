package boulderino.boulderino.service;

import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.repository.BoulderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoulderServiceTest {


@Mock
private BoulderRepository boulderRepository;

@InjectMocks
private BoulderService boulderService;

@Test
void getAllBoulders_shouldReturnAllBoulders() {

    Boulder boulder1 = new Boulder();
    boulder1.setName("Boulder 1");

    Boulder boulder2 = new Boulder();
    boulder2.setName("Boulder 2");

    List<Boulder> boulders = List.of(boulder1, boulder2);

    when(boulderRepository.findAll()).thenReturn(boulders);

    List<Boulder> result = boulderService.getAllBoulders();

    assertEquals(2, result.size());
    assertEquals("Boulder 1", result.get(0).getName());
    assertEquals("Boulder 2", result.get(1).getName());
}


}
