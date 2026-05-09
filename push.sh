#!/bin/bash

# 🎨 Màu sắc cho log nhìn cho sướng mắt
GREEN='\033[0-52;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}==================================================${NC}"
echo -e "${GREEN}🚀 HUANG PUSH SCRIPT - KHỞI ĐỘNG ĐẨY CODE${NC}"
echo -e "${YELLOW}==================================================${NC}"

# 1. Kiểm tra tin nhắn commit
COMMIT_MSG=$1
if [ -z "$COMMIT_MSG" ]; then
    COMMIT_MSG="Update code: $(date +'%Y-%m-%d %H:%M:%S')"
    echo -e "${YELLOW}[INFO] Không có tin nhắn, dùng mặc định: ${COMMIT_MSG}${NC}"
fi

# 2. Quét thay đổi (Add)
echo -e "${YELLOW}[1/3] 🔍 Đang quét các thay đổi...${NC}"
git add .

# 3. Đóng gói (Commit)
echo -e "${YELLOW}[2/3] 📦 Đang đóng gói (Commit)...${NC}"
git commit -m "$COMMIT_MSG"

# 4. Bắn lên mây (Push)
echo -e "${YELLOW}[3/3] 🚀 Đang đẩy lên GitHub (Branch: main)...${NC}"
if git push origin main; then
    echo -e "${GREEN}==================================================${NC}"
    echo -e "${GREEN}✅ THÀNH CÔNG RỰC RỠ! Code đã lên GitHub.${NC}"
    echo -e "${GREEN}==================================================${NC}"
else
    echo -e "${RED}==================================================${NC}"
    echo -e "${RED}❌ THẤT BẠI! Kiểm tra lại kết nối hoặc SSH Key nha ný.${NC}"
    echo -e "${RED}==================================================${NC}"
    exit 1
fi
