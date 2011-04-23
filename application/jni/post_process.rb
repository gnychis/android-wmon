#!/usr/bin/ruby

while (true)
	v = `ps aux | grep ndk-build | wc -l`.to_i
	if(v==2)
		puts `cp -v ../libs/armeabi/pcapd ../res/raw`
		break
	end
end
