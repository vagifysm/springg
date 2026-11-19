package com.example.app.mapper;

import com.example.app.dto.response.UserResponse;
import com.example.app.entity.User;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);
}
