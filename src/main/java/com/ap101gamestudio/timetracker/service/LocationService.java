package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.EstadoResponse;
import com.ap101gamestudio.timetracker.dto.MunicipioResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class LocationService {

    private static final List<EstadoResponse> ESTADOS = List.of(
            new EstadoResponse("AC", "Acre"),
            new EstadoResponse("AL", "Alagoas"),
            new EstadoResponse("AP", "Amapá"),
            new EstadoResponse("AM", "Amazonas"),
            new EstadoResponse("BA", "Bahia"),
            new EstadoResponse("CE", "Ceará"),
            new EstadoResponse("DF", "Distrito Federal"),
            new EstadoResponse("ES", "Espírito Santo"),
            new EstadoResponse("GO", "Goiás"),
            new EstadoResponse("MA", "Maranhão"),
            new EstadoResponse("MT", "Mato Grosso"),
            new EstadoResponse("MS", "Mato Grosso do Sul"),
            new EstadoResponse("MG", "Minas Gerais"),
            new EstadoResponse("PA", "Pará"),
            new EstadoResponse("PB", "Paraíba"),
            new EstadoResponse("PR", "Paraná"),
            new EstadoResponse("PE", "Pernambuco"),
            new EstadoResponse("PI", "Piauí"),
            new EstadoResponse("RJ", "Rio de Janeiro"),
            new EstadoResponse("RN", "Rio Grande do Norte"),
            new EstadoResponse("RS", "Rio Grande do Sul"),
            new EstadoResponse("RO", "Rondônia"),
            new EstadoResponse("RR", "Roraima"),
            new EstadoResponse("SC", "Santa Catarina"),
            new EstadoResponse("SP", "São Paulo"),
            new EstadoResponse("SE", "Sergipe"),
            new EstadoResponse("TO", "Tocantins")
    );

    private static final String IBGE_API_URL = "https://servicodados.ibge.gov.br/api/v1/localidades/estados/%s/municipios";

    private final RestTemplate restTemplate;

    public LocationService() {
        this.restTemplate = new RestTemplate();
    }

    public List<EstadoResponse> fetchEstados() {
        return ESTADOS;
    }

    public List<MunicipioResponse> fetchMunicipiosByUf(String uf) {
        try {
            String url = String.format(IBGE_API_URL, uf);
            ResponseEntity<List<IbgeMunicipioResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() == null) {
                return Collections.emptyList();
            }

            return response.getBody().stream()
                    .map(m -> new MunicipioResponse(String.valueOf(m.id()), m.nome(), uf))
                    .sorted(Comparator.comparing(MunicipioResponse::nome))
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private record IbgeMunicipioResponse(long id, String nome) {}
}
