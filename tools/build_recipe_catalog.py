"""Extract factual template/recipe metadata from pinned javap listings (no server code ships)."""
import re, json, pathlib, sys

def types(desc):
    return re.findall(r'\[*L[^;]+;|\[*[BCDFIJSZ]', desc)

def extract(path):
    stack, local, templates, entries = [], {}, {}, []
    def pop(): return stack.pop() if stack else None
    for line in pathlib.Path(path).read_text(encoding='utf-8-sig').splitlines():
        m=re.match(r'\s*\d+: (\w+)\s*(.*?)\s*(?:// (.*))?$',line)
        if not m: continue
        op,arg,comment=m.groups(); comment=comment or ''
        try:
            if op.startswith(('iconst_','fconst_','lconst_','dconst_')):
                v=op.split('_')[1];stack.append(-1 if v=='m1' else float(v) if op[0] in 'fd' else int(v))
            elif op in ('bipush','sipush'): stack.append(int(arg))
            elif op.startswith('ldc'):
                k,_,v=comment.partition(' ')
                stack.append(v if k=='String' else float(v.rstrip('fdl')) if k in ('int','float','long','double') else None)
            elif op=='aconst_null': stack.append(None)
            elif re.match(r'[iafl d]load_?\d*',op): stack.append(local.get(int(op.split('_')[1] if '_' in op else arg)))
            elif re.match(r'[iafld]store_?\d*',op): local[int(op.split('_')[1] if '_' in op else arg)]=pop()
            elif op=='dup': stack.append(stack[-1] if stack else None)
            elif op in ('newarray','anewarray'): stack.append([None]*max(0,min(10000,int(pop() or 0))))
            elif op in ('sastore','bastore','iastore','aastore'):
                v,ix,arr=pop(),pop(),pop()
                if isinstance(arr,list) and isinstance(ix,int) and 0<=ix<len(arr):arr[ix]=v
            elif op=='new': stack.append({'class':comment.removeprefix('class ')})
            elif op=='getstatic': stack.append(comment.split(':')[0].rsplit('.',1)[-1])
            elif op=='getfield': pop();stack.append(None)
            elif op=='putfield':
                value,obj=pop(),pop()
                if isinstance(obj,dict) and 'sizeString:' in comment:obj['size']=value
            elif op=='putstatic' or op=='pop':pop()
            elif op.startswith('invoke'):
                sig=re.search(r'([^\s]+):\((.*?)\)(.+)',comment)
                if not sig:continue
                name,params,result=sig.groups();ts=types(params);args=[pop() for _ in ts][::-1]
                obj=None if op=='invokestatic' else pop();ret=None
                if 'createItemTemplate' in name and len(args)>21 and any(isinstance(v,str) for v in args[1:3]):
                    # Some overloads insert a size parameter before the name.
                    ni=next(i for i,t in enumerate(ts) if t=='Ljava/lang/String;')
                    fi=next(i for i,t in enumerate(ts) if t=='F')
                    ai=next(i for i,t in enumerate(ts) if t=='[S')
                    ret={'id':int(args[0]),'name':args[ni],'plural':args[ni+1],'icon':args[ai+1],
                         'model':args[fi-1],'difficulty':float(args[fi]),'weight':int(args[fi+1]),'material':int(args[fi+2]),'flags':args[ai] or []}
                    templates[ret['id']]=ret
                elif ('createSimpleEntry' in name or 'createAdvancedEntry' in name) and len(args)>=10 and all(isinstance(v,(int,float)) for v in args[:6]):
                    ret={'skill':int(args[0]),'source':int(args[1]),'target':int(args[2]),'result':int(args[3]),
                         'depleteSource':bool(args[4]),'depleteTarget':bool(args[5]),'loss':args[6], 'destroyBoth':bool(args[7]),
                         'advanced':'createAdvancedEntry' in name,'category':str(args[-1] or ''),'requirements':[], 'sourceMaterial':0,'targetMaterial':0,
                         'sourceWeight':-1,'targetWeight':-1}
                    entries.append(ret)
                elif 'CreationRequirement."<init>"' in name and isinstance(obj,dict):
                    obj.update({'number':args[0],'template':args[1],'count':args[2],'consumed':bool(args[3])})
                elif isinstance(obj,dict):
                    if name.endswith('.setSizeString'):obj['size']=args[0]
                    if name.endswith('.addRequirement') and args and isinstance(args[0],dict):obj.get('requirements',[]).append(args[0]);ret=obj
                    for suffix,key in [('setDepleteFromSource','sourceWeight'),('setDepleteFromTarget','targetWeight'),
                                       ('setObjectSourceMaterial','sourceMaterial'),('setObjectTargetMaterial','targetMaterial')]:
                        if name.endswith('.'+suffix):obj[key]=args[0]
                if result!='V':stack.append(ret)
            elif op.startswith('if_icmp') or op.startswith('if_acmp'):pop();pop()
            elif op.startswith('if') or op in ('tableswitch','lookupswitch'):pop()
            elif op in ('iadd','isub','imul','idiv','fadd','fsub','fmul','fdiv'):
                b,a=pop(),pop();stack.append({'add':lambda:a+b,'sub':lambda:a-b,'mul':lambda:a*b,'div':lambda:a/b}[op[1:]]() if isinstance(a,(int,float)) and isinstance(b,(int,float)) and (b or op[-3:]!='div') else None)
            elif op.endswith('return') or op=='athrow':stack.clear()
        except (TypeError,ValueError,IndexError,StopIteration):
            stack.clear()
    return templates,entries

if __name__=='__main__':
    source=pathlib.Path(sys.argv[1]); destination=pathlib.Path(sys.argv[2]);destination.mkdir(parents=True,exist_ok=True)
    templates,_=extract(source/'Templates.txt');_,entries=extract(source/'Entries.txt')
    with (destination/'templates.tsv').open('w',encoding='utf-8',newline='\n') as out:
        for k,t in sorted(templates.items()):
            prefix=(str(t.get('size','')).strip()+' ').lstrip()
            out.write('\t'.join(str(v) for v in [k,prefix+t['name'],prefix+t['plural'],int(t['icon']),t['weight'],t['material'],t['model'],','.join(str(x) for x in t['flags']),t['difficulty']])+'\n')
    entries=[e for e in entries if all(e[x] in templates for x in ['source','target','result'])]
    with (destination/'entries.tsv').open('w',encoding='utf-8',newline='\n') as out:
        for e in entries:
            reqs=';'.join(','.join(str(int(r[x])) for x in ['number','template','count','consumed']) for r in e['requirements'] if all(isinstance(r.get(x),(int,float,bool)) for x in ['number','template','count','consumed']))
            out.write('\t'.join(str(v) for v in [e['result'],e['source'],e['target'],int(e['depleteSource']),int(e['depleteTarget']),e['sourceWeight'],e['targetWeight'],int(e['sourceMaterial'] or 0),int(e['targetMaterial'] or 0),e['skill'],int(e['advanced']),reqs,int(e['destroyBoth']),e['category'],e['loss']])+'\n')
    print(f'Extracted {len(templates)} templates, {len(entries)} creation entries')
    for ident in [7,9,22,24,74,145,178,185,186,236]:print(templates.get(ident))
