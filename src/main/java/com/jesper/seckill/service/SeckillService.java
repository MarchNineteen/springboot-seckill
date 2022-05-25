package com.jesper.seckill.service;

import com.jesper.seckill.bean.OrderInfo;
import com.jesper.seckill.bean.SeckillOrder;
import com.jesper.seckill.bean.User;
import com.jesper.seckill.redis.RedisService;
import com.jesper.seckill.redis.SeckillKey;
import com.jesper.seckill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by jiangyunxiong on 2018/5/23.
 */
@Service
public class SeckillService {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisService redisService;

    //保证这三个操作，减库存 下订单 写入秒杀订单是一个事物
    @Transactional
    public OrderInfo seckill(User user, GoodsVo goods) {
        //减库存
        boolean success = goodsService.reduceStock(goods);
        if (success) {
            //下订单 写入秒杀订单
            return orderService.createOrder(user, goods);
        } else {
            // 库存不足
            setGoodsOver(goods.getId(), user.getId());
            return null;
        }
    }

    public long getSeckillResult(long userId, long goodsId) {
        // mq有执行延迟 延迟去查询秒杀结果
        SeckillOrder order = orderService.getOrderByUserIdGoodsId(userId, goodsId);
        if (order != null) {
            return order.getOrderId();
        } else {
            // 再判断库存
            boolean isOver = getGoodsOver(goodsId);
            if(isOver) {
                return -1;
            }else {
                return 0;
            }
        }
    }

    private void setGoodsOver(Long goodsId, Long userId) {
        redisService.set(SeckillKey.isGoodsOver, "" + goodsId + "_" + userId, true);
    }

    /**
     * 库存不够的结束
     *
     * @return
     */
    private boolean getGoodsOver(long goodsId) {
        return redisService.exists(SeckillKey.isGoodsOver, ""+goodsId);
    }

}
