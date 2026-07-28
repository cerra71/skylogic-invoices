/**
 * Base class defining common conversion methods between entities
 * and their corresponding Data Transfer Objects.
 *
 * @param <E> entity type
 * @param <DTO> data transfer object type
 */


package com.skylogic.invoice.mapper;

import java.util.List;

public abstract class AbstractMapper<E, DTO> {

    // TO DTO

    /**
     * Converts an entity into its corresponding DTO.
     *
     * @param entity entity to convert
     * @return corresponding DTO
     */
    public abstract DTO toDTO(E entity);


    /**
     * Converts a list of entities into the corresponding DTO list.
     *
     * @param entities entities to convert
     * @return corresponding DTO list
     */
    public abstract List<DTO> toDTOs(List<E> entities);


    // TO ENTITY

    /**
     * Converts a DTO into its corresponding entity.
     *
     * @param dto DTO to convert
     * @return corresponding entity
     */
    public abstract E toEntity(DTO dto);

    /**
     * Converts a list of DTOs into the corresponding entity list.
     *
     * @param dtos DTOs to convert
     * @return corresponding entity list
     */
    public abstract List<E> toEntities(List<DTO> dtos);
}
