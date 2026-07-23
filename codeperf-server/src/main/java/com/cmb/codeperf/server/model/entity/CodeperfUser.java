package com.cmb.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("codeperf_user")
public class CodeperfUser {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业用户唯一标识 */
    @TableField("user_id")
    private String userId;

    /** 用户姓名 */
    @TableField("user_name")
    private String userName;

    /** SAP人员编号 */
    @TableField("sap_id")
    private String sapId;

    /** 用户邮箱 */
    private String email;

    /** 即时通讯账号 */
    @TableField("im_account")
    private String imAccount;

    /** 用户状态 */
    private String status;
}

