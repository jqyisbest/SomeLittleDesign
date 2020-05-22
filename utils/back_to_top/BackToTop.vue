<template>
  <transition name="el-fade-in">
    <div class="page-up" @click="scrollToTop" v-show="toTopShow">
      <i class="el-icon-caret-top"></i>
    </div>
  </transition>
<!--  <div class="page-up" @click="scrollToTop" v-show="toTopShow">-->
<!--    <i class="el-icon-caret-top"></i>-->
<!--  </div>-->
</template>

<script>
    export default {
        name: "BackToTop",
        mounted: function () {
            this.$nextTick(function () {
                window.addEventListener('scroll', this.handleScroll, true);
            });
        },
        destroyed: function () {
            window.addEventListener('scroll', this.handleScroll, true);
        },
        data: function () {
            return {
                toTopShow: false
            }
        },
        methods: {
            handleScroll: function () {
                let dom = document.scrollingElement;
                console.log(dom.scrollTop);
                this.scrollTop = dom.scrollTop;
                if (this.scrollTop > 580) {
                    this.toTopShow = true;
                } else {
                    this.toTopShow = false;
                }
            },
            scrollToTop: function () {
                let timer = null;
                let _this = this;
                cancelAnimationFrame(timer);
                timer = requestAnimationFrame(function fn() {
                    if (_this.scrollTop > 5000) {
                        _this.scrollTop -= 1000;
                        document.scrollingElement.scrollTop =
                            _this.scrollTop;
                        timer = requestAnimationFrame(fn);
                    } else if (_this.scrollTop > 1000 && _this.scrollTop <= 5000) {
                        _this.scrollTop -= 500;
                        document.scrollingElement.scrollTop =
                            _this.scrollTop;
                        timer = requestAnimationFrame(fn);
                    } else if (_this.scrollTop > 200 && _this.scrollTop <= 1000) {
                        _this.scrollTop -= 100;
                        document.scrollingElement.scrollTop =
                            _this.scrollTop;
                        timer = requestAnimationFrame(fn);
                    } else if (_this.scrollTop > 50 && _this.scrollTop <= 200) {
                        _this.scrollTop -= 10;
                        document.scrollingElement.scrollTop =
                            _this.scrollTop;
                        timer = requestAnimationFrame(fn);
                    } else if (_this.scrollTop > 0 && _this.scrollTop <= 50) {
                        _this.scrollTop -= 5;
                        document.scrollingElement.scrollTop =
                            _this.scrollTop;
                        timer = requestAnimationFrame(fn);
                    } else {
                        cancelAnimationFrame(timer);
                        _this.toTopShow = false;
                    }
                });
            }
        }
    }
</script>

<style scoped lang="css">
  .page-up {
    background-color: #fff;
    position: fixed;
    right: 40px;
    bottom: 40px;
    width: 40px;
    height: 40px;
    border-radius: 20px;
    cursor: pointer;
    transition: .3s;
    box-shadow: 0 3px 6px rgba(0, 0, 0, .5);
    opacity: .5;
    z-index: 100;
  }
  .page-up :hover {
    opacity: 1;
  }
  .el-icon-caret-top {
    color: #409eff;
    display: block;
    line-height: 40px;
    text-align: center;
    font-size: 18px;
  }
  p {
    display: none;
    text-align: center;
    color: #fff;
  }
</style>