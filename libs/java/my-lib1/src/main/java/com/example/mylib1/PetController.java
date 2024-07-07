package com.example.mylib1;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.PetsApi;
import org.openapitools.model.PetDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PetController implements PetsApi {
  private final HelloService helloService;


  @Override
  public ResponseEntity<Void> createPets(PetDTO petDTO) {
    return null;
  }

  @Override
  public ResponseEntity<List<PetDTO>> listPets(Integer limit) {
    return null;
  }

  @Override
  public ResponseEntity<PetDTO> showPetById(String petId) {
    return null;
  }
}
