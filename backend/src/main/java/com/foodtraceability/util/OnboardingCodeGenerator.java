package com.foodtraceability.util;

import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.OnboardingArchiveMapper;
import com.foodtraceability.mapper.UserMapper;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * 入职档案编号和用户名生成工具
 */
@Component
public class OnboardingCodeGenerator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OnboardingCodeGenerator.class);
    private final OnboardingArchiveMapper archiveMapper;
    private final UserMapper userMapper;

    /**
     * 生成员工编号
     * 格式：EMP + 年份(4位) + 序号(4位)
     * 示例：EMP2024001, EMP2024002
     *
     * @return 员工编号
     */
    public String generateEmployeeCode() {
        int currentYear = LocalDate.now().getYear();
        String yearPrefix = "EMP" + currentYear;
        // 查询当年最大的序号（使用原生SQL，不受逻辑删除影响）
        // 因为唯一索引包含已删除记录，需要查询所有记录避免冲突
        Integer maxSeq = archiveMapper.selectMaxEmployeeCodeSeq(yearPrefix);
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;
        // 格式化为4位序号，不足补0
        String employeeCode = String.format("%s%04d", yearPrefix, nextSeq);
        log.info("生成员工编号：{}", employeeCode);
        return employeeCode;
    }

    /**
     * 生成预设用户名
     * 规则：姓名拼音首字母 + 序号（避免重名）
     * 示例：张三 -> zs001, 李四 -> ls001
     *
     * @param name 真实姓名
     * @return 预设用户名
     */
    public String generatePresetUsername(String name) {
        if (name == null || name.isEmpty()) {
            return "user001";
        }
        // 获取姓名拼音首字母
        String prefix = getPinyinInitials(name);
        // 查询该前缀当前最大序号（使用原生SQL，不受逻辑删除影响）
        Integer maxSeq = archiveMapper.selectMaxPresetUsernameSeq(prefix);
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;
        // 格式化为3位序号，不足补0
        String presetUsername = String.format("%s%03d", prefix, nextSeq);
        // 检查是否与现有用户表冲突
        if (isUsernameExists(presetUsername)) {
            // 如果冲突，递增序号直到不冲突
            while (isUsernameExists(presetUsername)) {
                nextSeq++;
                presetUsername = String.format("%s%03d", prefix, nextSeq);
            }
        }
        log.info("生成预设用户名：{}", presetUsername);
        return presetUsername;
    }

    /**
     * 检查用户名是否已存在
     */
    private boolean isUsernameExists(String username) {
        // 检查用户表
        long userCount = userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.foodtraceability.entity.User>().eq("username", username));
        // 检查档案表的预设用户名（使用原生SQL，不受逻辑删除影响）
        long archiveCount = archiveMapper.countByUsername(username);
        return userCount > 0 || archiveCount > 0;
    }

    /**
     * 获取中文姓名的拼音首字母
     * 示例：张三 -> zs, 欧阳锋 -> oyf
     *
     * @param name 中文姓名
     * @return 拼音首字母
     */
    private String getPinyinInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "u";
        }
        StringBuilder initials = new StringBuilder();
        // 简单处理：取每个汉字的首字母
        // 注意：这里使用简化版，实际项目中可以使用pinyin4j等库
        for (char c : name.toCharArray()) {
            String initial = getSinglePinyinInitial(c);
            if (initial != null) {
                initials.append(initial);
            }
        }
        // 限制长度，最多取4个字母
        String result = initials.toString().toLowerCase();
        if (result.length() > 4) {
            result = result.substring(0, 4);
        }
        return result.isEmpty() ? "u" : result;
    }

    /**
     * 获取单个字符的拼音首字母（简化版）
     */
    private String getSinglePinyinInitial(char c) {
        // 如果是英文字母，直接返回
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return String.valueOf(Character.toLowerCase(c));
        }
        // 常见姓氏映射（简化处理）
        String[][] pinyinMap = {{"阿", "a"}, {"艾", "a"}, {"安", "a"}, {"奥", "a"}, {"巴", "b"}, {"白", "b"}, {"班", "b"}, {"包", "b"}, {"保", "b"}, {"鲍", "b"}, {"贝", "b"}, {"毕", "b"}, {"边", "b"}, {"卞", "b"}, {"蔡", "c"}, {"曹", "c"}, {"岑", "c"}, {"常", "c"}, {"车", "c"}, {"陈", "c"}, {"成", "c"}, {"程", "c"}, {"池", "c"}, {"迟", "c"}, {"储", "c"}, {"楚", "c"}, {"褚", "c"}, {"达", "d"}, {"戴", "d"}, {"单", "d"}, {"邓", "d"}, {"狄", "d"}, {"丁", "d"}, {"董", "d"}, {"窦", "d"}, {"杜", "d"}, {"段", "d"}, {"鄂", "e"}, {"恩", "e"}, {"儿", "e"}, {"樊", "f"}, {"范", "f"}, {"方", "f"}, {"房", "f"}, {"费", "f"}, {"丰", "f"}, {"冯", "f"}, {"凤", "f"}, {"傅", "f"}, {"符", "f"}, {"甘", "g"}, {"高", "g"}, {"葛", "g"}, {"耿", "g"}, {"龚", "g"}, {"顾", "g"}, {"关", "g"}, {"官", "g"}, {"管", "g"}, {"桂", "g"}, {"郭", "g"}, {"哈", "h"}, {"海", "h"}, {"韩", "h"}, {"杭", "h"}, {"郝", "h"}, {"何", "h"}, {"和", "h"}, {"贺", "h"}, {"赫", "h"}, {"侯", "h"}, {"洪", "h"}, {"胡", "h"}, {"花", "h"}, {"华", "h"}, {"黄", "h"}, {"霍", "h"}, {"姬", "j"}, {"吉", "j"}, {"纪", "j"}, {"季", "j"}, {"贾", "j"}, {"简", "j"}, {"江", "j"}, {"姜", "j"}, {"蒋", "j"}, {"金", "j"}, {"焦", "j"}, {"靳", "j"}, {"景", "j"}, {"居", "j"}, {"卡", "k"}, {"开", "k"}, {"凯", "k"}, {"柯", "k"}, {"孔", "k"}, {"寇", "k"}, {"匡", "k"}, {"蓝", "l"}, {"郎", "l"}, {"劳", "l"}, {"乐", "l"}, {"雷", "l"}, {"冷", "l"}, {"黎", "l"}, {"李", "l"}, {"厉", "l"}, {"利", "l"}, {"连", "l"}, {"廉", "l"}, {"梁", "l"}, {"廖", "l"}, {"林", "l"}, {"凌", "l"}, {"刘", "l"}, {"柳", "l"}, {"龙", "l"}, {"娄", "l"}, {"卢", "l"}, {"鲁", "l"}, {"陆", "l"}, {"路", "l"}, {"吕", "l"}, {"罗", "l"}, {"骆", "l"}, {"麻", "m"}, {"马", "m"}, {"麦", "m"}, {"满", "m"}, {"毛", "m"}, {"茅", "m"}, {"梅", "m"}, {"孟", "m"}, {"米", "m"}, {"苗", "m"}, {"明", "m"}, {"莫", "m"}, {"缪", "m"}, {"穆", "m"}, {"那", "n"}, {"南", "n"}, {"倪", "n"}, {"聂", "n"}, {"欧", "o"}, {"欧阳", "o"}, {"潘", "p"}, {"庞", "p"}, {"裴", "p"}, {"彭", "p"}, {"皮", "p"}, {"平", "p"}, {"蒲", "p"}, {"朴", "p"}, {"戚", "q"}, {"齐", "q"}, {"祁", "q"}, {"钱", "q"}, {"乔", "q"}, {"秦", "q"}, {"丘", "q"}, {"邱", "q"}, {"裘", "q"}, {"曲", "q"}, {"冉", "r"}, {"饶", "r"}, {"任", "r"}, {"戎", "r"}, {"荣", "r"}, {"容", "r"}, {"阮", "r"}, {"芮", "r"}, {"撒", "s"}, {"萨", "s"}, {"赛", "s"}, {"桑", "s"}, {"沙", "s"}, {"山", "s"}, {"商", "s"}, {"尚", "s"}, {"韶", "s"}, {"邵", "s"}, {"申", "s"}, {"沈", "s"}, {"盛", "s"}, {"施", "s"}, {"石", "s"}, {"史", "s"}, {"舒", "s"}, {"司", "s"}, {"宋", "s"}, {"苏", "s"}, {"孙", "s"}, {"索", "s"}, {"塔", "t"}, {"台", "t"}, {"太", "t"}, {"泰", "t"}, {"谈", "t"}, {"汤", "t"}, {"唐", "t"}, {"陶", "t"}, {"滕", "t"}, {"田", "t"}, {"童", "t"}, {"屠", "t"}, {"万", "w"}, {"汪", "w"}, {"王", "w"}, {"韦", "w"}, {"卫", "w"}, {"魏", "w"}, {"温", "w"}, {"文", "w"}, {"闻", "w"}, {"翁", "w"}, {"巫", "w"}, {"乌", "w"}, {"吴", "w"}, {"伍", "w"}, {"武", "w"}, {"西", "x"}, {"席", "x"}, {"夏", "x"}, {"冼", "x"}, {"项", "x"}, {"萧", "x"}, {"解", "x"}, {"谢", "x"}, {"辛", "x"}, {"邢", "x"}, {"熊", "x"}, {"徐", "x"}, {"许", "x"}, {"薛", "x"}, {"雅", "y"}, {"严", "y"}, {"颜", "y"}, {"晏", "y"}, {"阳", "y"}, {"杨", "y"}, {"叶", "y"}, {"伊", "y"}, {"易", "y"}, {"殷", "y"}, {"尹", "y"}, {"应", "y"}, {"尤", "y"}, {"于", "y"}, {"余", "y"}, {"俞", "y"}, {"虞", "y"}, {"宇", "y"}, {"禹", "y"}, {"玉", "y"}, {"郁", "y"}, {"喻", "y"}, {"元", "y"}, {"袁", "y"}, {"岳", "y"}, {"云", "y"}, {"臧", "z"}, {"曾", "z"}, {"查", "z"}, {"翟", "z"}, {"詹", "z"}, {"张", "z"}, {"章", "z"}, {"赵", "z"}, {"甄", "z"}, {"郑", "z"}, {"钟", "z"}, {"仲", "z"}, {"周", "z"}, {"朱", "z"}, {"庄", "z"}, {"卓", "z"}, {"宗", "z"}, {"邹", "z"}, {"祖", "z"}};
        String charStr = String.valueOf(c);
        for (String[] mapping : pinyinMap) {
            if (mapping[0].equals(charStr)) {
                return mapping[1];
            }
        }
        // 如果找不到映射，返回null（会被忽略）
        return null;
    }

    public OnboardingCodeGenerator(final OnboardingArchiveMapper archiveMapper, final UserMapper userMapper) {
        this.archiveMapper = archiveMapper;
        this.userMapper = userMapper;
    }
}
