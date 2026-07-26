package com.skylogic.invoice.mapper;

import java.util.List;

public abstract class AbstractMapper<E, DTO> {

    // TO DTO
    public abstract DTO toDTO(E entity);

    public abstract List<DTO> toDTOs(List<E> entities);

    // TO ENTITY		
    public abstract E toEntity(DTO dto);

    public abstract List<E> toEntities(List<DTO> dtos);
}
