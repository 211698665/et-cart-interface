package cn.henu.cart.service;

import java.util.List;

import cn.henu.common.utils.EtResult;
import cn.henu.pojo.TbItem;

public interface CartService {

	EtResult addCart(long userId,long itemId,int num);
	//合并购物车
	EtResult mergeCart(long userId,List<TbItem> list);
	//取购物车列表
	List<TbItem> getCartList(long userId);
	//更新购物车的数量
	EtResult updateCartNum(long userId,long itemId,int num);
	//删除商品
	EtResult deleteCartItem(long userId,long itemId);
	//清空购物车
	EtResult clearCartItem(long userId);
}
