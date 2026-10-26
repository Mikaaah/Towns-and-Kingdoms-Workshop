package nl.townsandkingdoms.core.projector.client;

import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.ChatFormatting;

/** Plain text plus familiar & color/style codes, hex colors, **bold**, *italic*. No executable click events. */
public final class FormattedText {
    private FormattedText() {}
    public static Component parse(String source) {
        var result=Component.empty(); var part=new StringBuilder(); Style style=Style.EMPTY;
        for(int i=0;i<source.length();) {
            char c=source.charAt(i);
            if(c=='\\' && i+1<source.length()) { char n=source.charAt(i+1); if(n=='n'){part.append('\n');i+=2;continue;} if(n=='*'||n=='&'||n=='\\'){part.append(n);i+=2;continue;} }
            int skip=0; Style next=null;
            if((c=='&'||c=='§') && i+1<source.length()) {
                char code=Character.toLowerCase(source.charAt(i+1));
                if(code=='#' && i+8<=source.length()) {
                    try {next=style.withColor(Integer.parseInt(source.substring(i+2,i+8),16));skip=8;} catch(NumberFormatException ignored){}
                } else {
                    ChatFormatting f=ChatFormatting.getByCode(code);
                    if(f!=null) { next=f==ChatFormatting.RESET?Style.EMPTY:f.isColor()?Style.EMPTY.applyFormat(f):style.applyFormat(f);skip=2; }
                }
            } else if(c=='*') {
                if(i+1<source.length()&&source.charAt(i+1)=='*'){next=style.withBold(!style.isBold());skip=2;}
                else {next=style.withItalic(!style.isItalic());skip=1;}
            }
            if(next!=null){ if(!part.isEmpty()){result.append(Component.literal(part.toString()).setStyle(style));part.setLength(0);} style=next;i+=skip; }
            else {part.append(c);i++;}
        }
        if(!part.isEmpty()) result.append(Component.literal(part.toString()).setStyle(style));
        return result;
    }
}
