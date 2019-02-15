package cn.henu.cart.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.henu.cart.service.CartService;
import cn.henu.common.jedis.JedisClient;
import cn.henu.common.jedis.JedisClientCluster;
import cn.henu.common.utils.EtResult;
import cn.henu.common.utils.JsonUtils;
import cn.henu.mapper.TbItemMapper;
import cn.henu.pojo.TbItem;

/**
 * 处理添加购物车
 * @author syw
 *
 */
@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private TbItemMapper tbItemMapper;
	@Autowired
	private JedisClientCluster jedisClientCluster;
	@Value("${REDIS_CART_PRE}")
	private String REDIS_CART_PRE;
	@Override
	public EtResult addCart(long userId, long itemId,int num) {
		// 向redis中添加购物车
		//数据类型是hash,key为userId, hash里面的field为itemId，value为商品信息
		//判断商品是否存在，如果存在,数量相加
		Boolean hexists = jedisClientCluster.hexists(REDIS_CART_PRE + ":" + userId, itemId + "");
		if (hexists) {
			String json = jedisClientCluster.hget(REDIS_CART_PRE + ":" + userId, itemId + "");
			//把json转换成TbItem
			TbItem item = JsonUtils.jsonToPojo(json, TbItem.class);
			item.setNum(item.getNum() + num);
			//写回redis
			jedisClientCluster.hset(REDIS_CART_PRE + ":" + userId, itemId + "", JsonUtils.objectToJson(item));
			return EtResult.ok();
		}
		//如果不存在，根据商品id取出商品信息,
		TbItem tbItem = tbItemMapper.selectByPrimaryKey(itemId);
		//设置购物车数量
		tbItem.setNum(num);
		//取出一张图片
		String images=tbItem.getImage();
		if(StringUtils.isNoneBlank(images)) {
			tbItem.setImage(images.split(",")[0]);
		}
		//添加到购物车列表
		jedisClientCluster.hset(REDIS_CART_PRE+":"+userId, itemId+"" ,JsonUtils.objectToJson(tbItem));
		return EtResult.ok();
	}
	@Override
	public EtResult mergeCart(long userId, List<TbItem> list) {
		//遍历商品列表，
		//把列表添加到购物车
		//判断购物车是否有此商品
		//如果有数量相加
		//如果没有添加一个新的商品
		for (TbItem tbItem : list) {
			addCart(userId,tbItem.getId(),tbItem.getNum());
		}
		//返回成功
		return EtResult.ok();
	}
	@Override
	public List<TbItem> getCartList(long userId) {
		List<String> jsonList = jedisClientCluster.hvals(REDIS_CART_PRE + ":" + userId);
		List<TbItem> itemList = new ArrayList<>();
		for (String string : jsonList) {
			//创建一个TbItem对象
			TbItem item = JsonUtils.jsonToPojo(string, TbItem.class);
			//添加到列表
			itemList.add(item);
		}
		return itemList;
	}
	@Override
	public EtResult updateCartNum(long userId, long itemId, int num) {
		//从redis中取出商品信息
		String json = jedisClientCluster.hget(REDIS_CART_PRE+":"+userId, itemId+"");
		//更新商品数量
		TbItem tbItem = JsonUtils.jsonToPojo(json, TbItem.class);
		tbItem.setNum(num);
		//写入redis
		jedisClientCluster.hset(REDIS_CART_PRE+":"+userId, itemId+"" ,JsonUtils.objectToJson(tbItem));
		return EtResult.ok();
	}
	@Override
	public EtResult deleteCartItem(long userId, long itemId) {
		// 删除购物车商品
		jedisClientCluster.hdel(REDIS_CART_PRE+":"+userId, itemId+"" );
		return EtResult.ok();
	}
	@Override
	public EtResult clearCartItem(long userId) {
		// 删除购物车信息
		jedisClientCluster.del(REDIS_CART_PRE+":"+userId);
		return EtResult.ok();
	}

}
