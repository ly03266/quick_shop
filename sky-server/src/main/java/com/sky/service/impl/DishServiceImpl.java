package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Transactional // 同时成功，同时失败
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        // 向菜品表插入一条数据
        dishMapper.insert(dish);
        // 获得自增ID
        Long dishId = dish.getId();
        // 从DTO中获取菜品对应的口味列表
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 判断是否有口味信息需要处理
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                // 遍历所有口味，为每个口味设置对应的菜品ID（建立关联关系）
                dishFlavor.setDishId(dishId);
            });
            // 向口味表插入n条数据 flavors与mapper.xml中参数名一致
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Transactional
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 开启分页
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        // 执行分页查询
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        // 封装分页结果
        return new PageResult(page.getTotal(),page.getResult());

    }

    /**
     * 批量删除菜品
     * @param ids
     */
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否能够删除--是否存在启售中的菜品
        for (Long id : ids){
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断当前菜品是否能够删除--是否被套餐关联了
        List<Long> setmealIds = setmealDishMapper.getSetmealIdByDishIds(ids);
        if (setmealIds != null && setmealIds.size() >0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 删除菜品表中的菜品数据
        for (Long id:ids){
            dishMapper.deleteById(id);
            // 删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }


    }

}
