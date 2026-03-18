# 学生管理系统（Flask + SQLite）

功能：
- 学生信息增删改查（CRUD）
- 关键字搜索（姓名/学号/班级）
- 列表分页

## 运行环境
- Windows 10/11
- Python 3.10+（推荐 3.11/3.12）

## 安装依赖

```bash
python -m venv .venv
.venv\Scripts\activate
python -m pip install -U pip
pip install -r requirements.txt
```

## 初始化数据库并启动

```bash
python app.py --init-db
python app.py
```

启动后访问：
- `http://127.0.0.1:5000`

## 数据字段
- 学号（唯一）
- 姓名
- 性别
- 年龄
- 班级
- 电话
- 备注

