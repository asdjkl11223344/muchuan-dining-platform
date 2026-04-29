param(
  [string]$TargetRoot = "D:\nginx前端运行环境\外卖项目\nginx-1.20.2\html\muchuan\img\menu"
)

$ErrorActionPreference = "Stop"

function Escape-Xml([string]$text) {
  if ($null -eq $text) { return "" }
  return $text.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;").Replace('"', "&quot;").Replace("'", "&apos;")
}

function Get-Theme([string]$theme) {
  switch ($theme) {
    "drink"   { return @{ Bg1 = "#F4E6D8"; Bg2 = "#E6B37B"; Accent = "#A85A2A"; Ink = "#3E2418"; Soft = "#FFF3E8" } }
    "staple"  { return @{ Bg1 = "#EFE7D5"; Bg2 = "#D8C19B"; Accent = "#8D6334"; Ink = "#40311E"; Soft = "#FFF8EC" } }
    "fish"    { return @{ Bg1 = "#F6DDD3"; Bg2 = "#D47E5A"; Accent = "#9A4028"; Ink = "#411D16"; Soft = "#FFF3EE" } }
    "veg"     { return @{ Bg1 = "#E3F0DA"; Bg2 = "#9BC481"; Accent = "#3D6B3E"; Ink = "#1F381E"; Soft = "#F5FFF1" } }
    "steam"   { return @{ Bg1 = "#E0EBEE"; Bg2 = "#9FB7BE"; Accent = "#45636B"; Ink = "#21343A"; Soft = "#F3FCFF" } }
    "bullfrog"{ return @{ Bg1 = "#E8E0F4"; Bg2 = "#B49AD9"; Accent = "#60428D"; Ink = "#2D2047"; Soft = "#F8F3FF" } }
    "soup"    { return @{ Bg1 = "#F6ECD9"; Bg2 = "#E2C27D"; Accent = "#9A7128"; Ink = "#463111"; Soft = "#FFF8EA" } }
    "setmeal" { return @{ Bg1 = "#E3EFE9"; Bg2 = "#A7C9B7"; Accent = "#1F5A56"; Ink = "#183635"; Soft = "#F3FCF8" } }
    default   { return @{ Bg1 = "#E8ECEF"; Bg2 = "#C0CBD3"; Accent = "#445865"; Ink = "#243039"; Soft = "#F6FAFC" } }
  }
}

function Get-Illustration([string]$icon, [hashtable]$theme) {
  $accent = $theme.Accent
  $soft = $theme.Soft
  switch ($icon) {
    "drink" {
      return @"
  <ellipse cx="872" cy="380" rx="130" ry="130" fill="$soft" fill-opacity="0.56"/>
  <path d="M835 230 L910 230 L885 470 Q880 500 846 500 Q812 500 806 470 Z" fill="$soft" stroke="$accent" stroke-width="12" stroke-linejoin="round"/>
  <rect x="846" y="186" width="12" height="58" rx="6" fill="$accent"/>
  <path d="M858 186 C884 146 916 140 940 166" stroke="$accent" stroke-width="10" fill="none" stroke-linecap="round"/>
  <circle cx="840" cy="326" r="12" fill="$accent" fill-opacity="0.16"/>
  <circle cx="875" cy="362" r="18" fill="$accent" fill-opacity="0.14"/>
"@
    }
    "staple" {
      return @"
  <ellipse cx="860" cy="402" rx="156" ry="84" fill="$accent" fill-opacity="0.18"/>
  <path d="M742 382 C742 314 792 268 860 268 C928 268 978 314 978 382 C978 454 930 500 860 500 C790 500 742 454 742 382 Z" fill="$accent"/>
  <path d="M772 362 C772 314 810 282 860 282 C910 282 948 314 948 362 C948 416 908 458 860 458 C812 458 772 416 772 362 Z" fill="$soft"/>
  <path d="M792 356 C820 330 844 326 860 344 C884 320 914 326 928 354" stroke="$accent" stroke-width="10" fill="none" stroke-linecap="round"/>
"@
    }
    "fish" {
      return @"
  <ellipse cx="860" cy="400" rx="174" ry="110" fill="$soft" fill-opacity="0.82"/>
  <path d="M760 396 C760 332 810 292 874 292 C924 292 962 316 984 356 L1028 340 L1000 388 L1028 434 L984 420 C960 468 922 492 874 492 C810 492 760 452 760 396 Z" fill="$accent"/>
  <circle cx="830" cy="372" r="8" fill="$soft"/>
  <path d="M804 416 C840 392 886 392 924 416" stroke="$soft" stroke-width="12" fill="none" stroke-linecap="round"/>
  <path d="M720 274 C760 236 804 222 850 230" stroke="$accent" stroke-width="10" fill="none" stroke-linecap="round" opacity="0.45"/>
"@
    }
    "veg" {
      return @"
  <ellipse cx="860" cy="414" rx="174" ry="84" fill="$soft" fill-opacity="0.82"/>
  <path d="M782 430 C744 382 752 320 814 298 C826 338 816 376 782 430 Z" fill="$accent"/>
  <path d="M874 442 C820 398 816 326 878 290 C902 342 900 390 874 442 Z" fill="$accent" fill-opacity="0.92"/>
  <path d="M946 420 C904 396 888 346 928 308 C968 340 982 390 946 420 Z" fill="$accent" fill-opacity="0.76"/>
  <path d="M860 298 C846 350 852 392 884 434" stroke="$soft" stroke-width="10" fill="none" stroke-linecap="round"/>
"@
    }
    "steam" {
      return @"
  <ellipse cx="860" cy="422" rx="176" ry="78" fill="$accent" fill-opacity="0.18"/>
  <rect x="744" y="316" width="232" height="132" rx="44" fill="$accent"/>
  <rect x="770" y="340" width="180" height="92" rx="30" fill="$soft"/>
  <path d="M808 306 C790 266 804 232 834 214" stroke="$accent" stroke-width="10" fill="none" stroke-linecap="round"/>
  <path d="M860 296 C842 248 862 218 894 198" stroke="$accent" stroke-width="10" fill="none" stroke-linecap="round"/>
  <path d="M916 306 C900 264 918 236 952 216" stroke="$accent" stroke-width="10" fill="none" stroke-linecap="round"/>
"@
    }
    "bullfrog" {
      return @"
  <ellipse cx="860" cy="408" rx="176" ry="94" fill="$soft" fill-opacity="0.82"/>
  <path d="M740 404 C740 326 794 274 860 274 C926 274 980 326 980 404 C980 458 940 498 860 498 C780 498 740 458 740 404 Z" fill="$accent"/>
  <circle cx="816" cy="356" r="18" fill="$soft"/>
  <circle cx="904" cy="356" r="18" fill="$soft"/>
  <circle cx="816" cy="356" r="8" fill="$accent"/>
  <circle cx="904" cy="356" r="8" fill="$accent"/>
  <path d="M810 424 C834 442 886 442 910 424" stroke="$soft" stroke-width="12" fill="none" stroke-linecap="round"/>
"@
    }
    "soup" {
      return @"
  <ellipse cx="860" cy="424" rx="180" ry="70" fill="$accent" fill-opacity="0.18"/>
  <path d="M740 354 C740 446 782 494 860 494 C938 494 980 446 980 354 Z" fill="$accent"/>
  <path d="M770 356 C772 420 804 456 860 456 C916 456 948 420 950 356 Z" fill="$soft"/>
  <path d="M804 322 C790 286 796 258 824 236" stroke="$accent" stroke-width="10" fill="none" stroke-linecap="round" opacity="0.45"/>
  <path d="M868 312 C856 274 864 248 892 224" stroke="$accent" stroke-width="10" fill="none" stroke-linecap="round" opacity="0.45"/>
"@
    }
    "setmeal" {
      return @"
  <rect x="714" y="256" width="296" height="224" rx="34" fill="$accent"/>
  <rect x="744" y="286" width="110" height="82" rx="20" fill="$soft"/>
  <rect x="870" y="286" width="110" height="82" rx="20" fill="$soft" fill-opacity="0.9"/>
  <rect x="744" y="382" width="236" height="68" rx="22" fill="$soft" fill-opacity="0.96"/>
  <circle cx="800" cy="326" r="18" fill="$accent" fill-opacity="0.28"/>
  <circle cx="926" cy="326" r="18" fill="$accent" fill-opacity="0.28"/>
  <path d="M792 414 H936" stroke="$accent" stroke-width="12" stroke-linecap="round"/>
"@
    }
    default {
      return @"
  <circle cx="860" cy="380" r="120" fill="$accent" fill-opacity="0.14"/>
  <rect x="742" y="286" width="236" height="188" rx="36" fill="$accent"/>
  <rect x="772" y="316" width="176" height="128" rx="24" fill="$soft"/>
"@
    }
  }
}

function New-MenuSvg($asset) {
  $theme = Get-Theme $asset.Theme
  $title = Escape-Xml $asset.Name
  $tag = Escape-Xml $asset.Tag
  $copy = Escape-Xml $asset.Copy
  $brand = if ($asset.Type -eq "setmeal") { "MUCHUAN SIGNATURE SET" } else { "MUCHUAN MENU ITEM" }
  $brand = Escape-Xml $brand
  $illustration = Get-Illustration $asset.Icon $theme
  $assetKey = "{0}-{1}" -f $asset.Type, $asset.Id
  $bg1 = $theme.Bg1
  $bg2 = $theme.Bg2
  $accent = $theme.Accent
  $ink = $theme.Ink
  $soft = $theme.Soft

  return @"
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900">
  <defs>
    <linearGradient id="bg-$assetKey" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="$bg1"/>
      <stop offset="100%" stop-color="$bg2"/>
    </linearGradient>
  </defs>
  <rect width="1200" height="900" rx="48" fill="url(#bg-$assetKey)"/>
  <circle cx="176" cy="168" r="120" fill="$soft" fill-opacity="0.28"/>
  <circle cx="1050" cy="154" r="88" fill="$soft" fill-opacity="0.2"/>
  <circle cx="1118" cy="748" r="120" fill="$soft" fill-opacity="0.18"/>
  <rect x="78" y="78" width="508" height="744" rx="36" fill="$soft" fill-opacity="0.88"/>
  <rect x="78" y="78" width="508" height="744" rx="36" fill="none" stroke="$accent" stroke-opacity="0.14" stroke-width="2"/>
  <text x="132" y="166" fill="$accent" font-size="24" font-family="'Microsoft YaHei','PingFang SC','Noto Sans SC',sans-serif" letter-spacing="4">$brand</text>
  <text x="132" y="282" fill="$ink" font-size="70" font-weight="700" font-family="'Microsoft YaHei','PingFang SC','Noto Sans SC',sans-serif">$title</text>
  <rect x="132" y="330" width="188" height="54" rx="27" fill="$accent"/>
  <text x="166" y="366" fill="$soft" font-size="24" font-weight="700" font-family="'Microsoft YaHei','PingFang SC','Noto Sans SC',sans-serif">$tag</text>
  <text x="132" y="452" fill="$ink" fill-opacity="0.82" font-size="32" font-family="'Microsoft YaHei','PingFang SC','Noto Sans SC',sans-serif">$copy</text>
  <rect x="132" y="544" width="272" height="18" rx="9" fill="$accent" fill-opacity="0.9"/>
  <rect x="132" y="590" width="336" height="18" rx="9" fill="$accent" fill-opacity="0.45"/>
  <rect x="132" y="636" width="212" height="18" rx="9" fill="$accent" fill-opacity="0.22"/>
  <text x="132" y="756" fill="$accent" fill-opacity="0.72" font-size="22" font-family="'Microsoft YaHei','PingFang SC','Noto Sans SC',sans-serif">muchuan dining platform</text>
$illustration
</svg>
"@
}

$assets = @(
  @{ Type = "dish"; Id = 46; Name = "陈皮凉茶"; Theme = "drink"; Tag = "草本饮品"; Copy = "回甘清爽，搭配重口主菜"; Icon = "drink" },
  @{ Type = "dish"; Id = 47; Name = "橙香汽水"; Theme = "drink"; Tag = "轻气泡"; Copy = "冰镇更清爽，适合午市"; Icon = "drink" },
  @{ Type = "dish"; Id = 48; Name = "精酿拉格"; Theme = "drink"; Tag = "晚市畅饮"; Copy = "麦香干净，适合分享"; Icon = "drink" },
  @{ Type = "dish"; Id = 49; Name = "五常香米饭"; Theme = "staple"; Tag = "主食加点"; Copy = "现蒸出锅，米粒饱满"; Icon = "staple" },
  @{ Type = "dish"; Id = 50; Name = "手作老面馒头"; Theme = "staple"; Tag = "手作主食"; Copy = "一份两只，适合搭锅"; Icon = "staple" },
  @{ Type = "dish"; Id = 51; Name = "金汤酸萝卜鱼锅"; Theme = "fish"; Tag = "鲜汤煨煮"; Copy = "酸香开胃，鱼肉鲜嫩"; Icon = "fish" },
  @{ Type = "dish"; Id = 52; Name = "招牌酸汤鮰鱼"; Theme = "fish"; Tag = "店内爆款"; Copy = "汤底厚重，复购率高"; Icon = "fish" },
  @{ Type = "dish"; Id = 53; Name = "藤椒鲜椒草鱼"; Theme = "fish"; Tag = "藤椒鱼锅"; Copy = "椒香充足，更下饭"; Icon = "fish" },
  @{ Type = "dish"; Id = 54; Name = "蒜香油麦菜"; Theme = "veg"; Tag = "时令小炒"; Copy = "脆嫩爽口，出餐快"; Icon = "veg" },
  @{ Type = "dish"; Id = 55; Name = "金蒜娃娃菜"; Theme = "veg"; Tag = "人气配菜"; Copy = "蒜香浓郁，桌边常点"; Icon = "veg" },
  @{ Type = "dish"; Id = 56; Name = "白灼西兰花"; Theme = "veg"; Tag = "轻负担"; Copy = "口味清爽，适合午餐"; Icon = "veg" },
  @{ Type = "dish"; Id = 57; Name = "炝锅圆白菜"; Theme = "veg"; Tag = "高峰爆品"; Copy = "锅气足，翻台快"; Icon = "veg" },
  @{ Type = "dish"; Id = 58; Name = "清蒸海鲈"; Theme = "steam"; Tag = "蒸烤功夫菜"; Copy = "主厨推荐，适合晚宴"; Icon = "steam" },
  @{ Type = "dish"; Id = 59; Name = "酱香东坡肘"; Theme = "steam"; Tag = "慢火煨制"; Copy = "肥瘦有层次，香气足"; Icon = "steam" },
  @{ Type = "dish"; Id = 60; Name = "梅干菜扣肉"; Theme = "steam"; Tag = "午市热销"; Copy = "经典下饭，接受度高"; Icon = "steam" },
  @{ Type = "dish"; Id = 61; Name = "剁椒鱼头"; Theme = "steam"; Tag = "分享菜"; Copy = "香辣平衡，适合多人"; Icon = "steam" },
  @{ Type = "dish"; Id = 62; Name = "藤椒牛蛙锅"; Theme = "bullfrog"; Tag = "铁锅牛蛙"; Copy = "高峰销量稳定"; Icon = "bullfrog" },
  @{ Type = "dish"; Id = 63; Name = "香辣牛蛙煲"; Theme = "bullfrog"; Tag = "招牌辣味"; Copy = "适合自定义辣度"; Icon = "bullfrog" },
  @{ Type = "dish"; Id = 64; Name = "仔姜牛蛙"; Theme = "bullfrog"; Tag = "夜宵高频"; Copy = "姜香醒胃，晚市强势"; Icon = "bullfrog" },
  @{ Type = "dish"; Id = 65; Name = "青花椒烤草鱼"; Theme = "fish"; Tag = "招牌鱼锅"; Copy = "门店代表菜，适合双人"; Icon = "fish" },
  @{ Type = "dish"; Id = 66; Name = "金尊江团鱼锅"; Theme = "fish"; Tag = "高客单价"; Copy = "商务聚餐常点"; Icon = "fish" },
  @{ Type = "dish"; Id = 67; Name = "鮰鱼豆花锅"; Theme = "fish"; Tag = "口感层次"; Copy = "豆花细腻，更适合分享"; Icon = "fish" },
  @{ Type = "dish"; Id = 68; Name = "紫菜蛋花汤"; Theme = "soup"; Tag = "汤羹小食"; Copy = "出餐快速，适合凑单"; Icon = "soup" },
  @{ Type = "dish"; Id = 69; Name = "平菇豆腐汤"; Theme = "soup"; Tag = "清爽汤羹"; Copy = "搭配重口味主菜更平衡"; Icon = "soup" },
  @{ Type = "dish"; Id = 70; Name = "招牌椒麻鱼片"; Theme = "fish"; Tag = "测试菜品"; Copy = "当前停售，保留展示图"; Icon = "fish" },
  @{ Type = "setmeal"; Id = 32; Name = "午市轻享双拼餐"; Theme = "setmeal"; Tag = "轻享单人餐"; Copy = "主菜+蔬菜+主食+汤羹"; Icon = "setmeal" },
  @{ Type = "setmeal"; Id = 33; Name = "主厨招牌单人餐"; Theme = "setmeal"; Tag = "高复购套餐"; Copy = "运营推荐位主打套餐"; Icon = "setmeal" },
  @{ Type = "setmeal"; Id = 34; Name = "周末双人分享餐"; Theme = "setmeal"; Tag = "分享聚会餐"; Copy = "双人聚餐组合，适合周末"; Icon = "setmeal" }
)

New-Item -ItemType Directory -Force -Path $TargetRoot | Out-Null

$written = 0

foreach ($asset in $assets) {
  $fileName = "{0}-{1}.svg" -f $asset.Type, $asset.Id
  $outputPath = Join-Path $TargetRoot $fileName
  $svg = New-MenuSvg $asset
  Set-Content -Path $outputPath -Value $svg -Encoding UTF8
  Write-Host ("generated {0} -> {1}" -f $asset.Name, $fileName)
  $written++
}

Write-Host ""
Write-Host ("generated: {0}" -f $written)
Write-Host ("target:    {0}" -f $TargetRoot)
